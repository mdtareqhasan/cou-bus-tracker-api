package com.cou.bustracker.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    private static final long TARGET_MAX_BYTES = 300L * 1024L;

    /**
     * Uploads an image to Cloudinary and returns its secure HTTPS URL.
     * Large images are automatically compressed before upload to keep them below
     * the 300 KB target while preserving readability for ID-card submissions.
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        ensureConfigured();
        byte[] uploadBytes = prepareUploadBytes(file);
        Map<String, Object> result = cloudinary.uploader().upload(uploadBytes, ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image"));
        return (String) result.get("secure_url");
    }

    private byte[] prepareUploadBytes(MultipartFile file) throws IOException {
        byte[] originalBytes = file.getBytes();
        if (originalBytes.length <= TARGET_MAX_BYTES) {
            return originalBytes;
        }

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (originalImage == null) {
            return originalBytes;
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        double scale = Math.min(1.0d, Math.sqrt((double) TARGET_MAX_BYTES / Math.max(1L, originalBytes.length)));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        for (float quality : new float[] { 0.82f, 0.74f, 0.66f, 0.58f, 0.50f, 0.42f, 0.34f }) {
            byte[] compressed = compressToJpeg(resized, quality);
            if (compressed.length <= TARGET_MAX_BYTES) {
                return compressed;
            }
        }

        return compressToJpeg(resized, 0.30f);
    }

    private byte[] compressToJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * Deletes an image from Cloudinary by its full URL or raw public id.
     */
    public void deleteImage(String imageUrlOrPublicId) {
        try {
            String publicId = extractPublicId(imageUrlOrPublicId);
            if (publicId == null || publicId.isBlank()) {
                log.warn("Skipping Cloudinary delete: no public id found in '{}'", imageUrlOrPublicId);
                return;
            }
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image"));
            log.debug("Cloudinary delete result for '{}': {}", publicId, result);
        } catch (Exception e) {
            log.warn("Failed to delete image '{}' from Cloudinary: {}", imageUrlOrPublicId, e.getMessage());
        }
    }

    private void ensureConfigured() {
        if (cloudName == null || cloudName.isBlank()) {
            throw new IllegalStateException(
                    "Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY " +
                            "and CLOUDINARY_API_SECRET before uploading images.");
        }
    }

    /**
     * Converts a Cloudinary URL like
     * https://res.cloudinary.com/<cloud>/image/upload/v123/folder/abc.png
     * into its public id: folder/abc
     * A raw public id (no scheme) is returned unchanged.
     */
    private String extractPublicId(String imageUrlOrPublicId) {
        if (imageUrlOrPublicId == null || !imageUrlOrPublicId.contains("://")) {
            return imageUrlOrPublicId;
        }
        try {
            String path = URI.create(imageUrlOrPublicId).getPath();
            String marker = "/image/upload/";
            int markerIndex = path.indexOf(marker);
            if (markerIndex < 0) {
                return null;
            }
            String rest = path.substring(markerIndex + marker.length());
            int slash = rest.indexOf('/');
            if (slash > 0 && rest.startsWith("v") && Character.isDigit(rest.charAt(1))) {
                rest = rest.substring(slash + 1);
            }
            int dot = rest.lastIndexOf('.');
            if (dot > 0) {
                rest = rest.substring(0, dot);
            }
            return rest;
        } catch (Exception e) {
            return imageUrlOrPublicId;
        }
    }
}