package com.cou.bustracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final CloudinaryService cloudinaryService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MIN_IMAGE_WIDTH = 300;
    private static final int MIN_IMAGE_HEIGHT = 200;
    private static final int MAX_IMAGE_WIDTH = 4000;
    private static final int MAX_IMAGE_HEIGHT = 4000;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png"
    );

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
    );

    /**
     * Stores an image in Cloudinary and returns its secure URL.
     */
    public String storeFile(MultipartFile file, String subDir) throws IOException {
        return cloudinaryService.uploadImage(file, subDir);
    }

    /**
     * Stores an ID card image with strict validation:
     * - Must not be empty
     * - Must be JPEG or PNG (by content type AND magic bytes)
     * - Must be within 5 MB
     * - Must be a readable image (width/height within bounds)
     */
    public String storeIdCard(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "ID card image is required. Please upload a clear photo of your university ID card."
            );
        }

        // --- File size check ---
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "ID card image must be smaller than 5 MB. Your file is " +
                            formatFileSize(file.getSize()) + "."
            );
        }

        // --- Content-type check ---
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Only JPG and PNG images are accepted for ID card upload. " +
                            "Received: " + (contentType == null ? "unknown" : contentType) + ". " +
                            "Please upload a clear photo of your ID card."
            );
        }

        // --- Magic-byte check (can't be spoofed with a renamed file) ---
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 3) {
                throw new IllegalArgumentException(
                        "The uploaded file is too small or corrupt. Please upload a valid ID card image."
                );
            }
            String detectedType = detectImageType(header);
            if (detectedType == null) {
                throw new IllegalArgumentException(
                        "The uploaded file is not a valid JPG or PNG image. " +
                                "Only university ID card photos are accepted."
                );
            }
        }

        // --- Decode & validate dimensions ---
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException(
                        "Could not read the uploaded image. Please upload a clear JPG or PNG " +
                                "photo of your university ID card."
                );
            }
            int w = image.getWidth();
            int h = image.getHeight();
            if (w < MIN_IMAGE_WIDTH || h < MIN_IMAGE_HEIGHT) {
                throw new IllegalArgumentException(
                        "ID card image is too small (" + w + "x" + h + " px). " +
                                "Minimum resolution is " + MIN_IMAGE_WIDTH + "x" + MIN_IMAGE_HEIGHT +
                                " px. Please upload a clearer photo."
                );
            }
            if (w > MAX_IMAGE_WIDTH || h > MAX_IMAGE_HEIGHT) {
                throw new IllegalArgumentException(
                        "ID card image is too large (" + w + "x" + h + " px). " +
                                "Maximum allowed resolution is " + MAX_IMAGE_WIDTH + "x" + MAX_IMAGE_HEIGHT + " px."
                );
            }
        }

        return cloudinaryService.uploadImage(file, subDir);
    }

    /**
     * Deletes an image from Cloudinary by URL or public id. Safe to call with
     * null / local paths / already-deleted images (no-op).
     */
    public void deleteFile(String fileUrlOrPublicId) {
        cloudinaryService.deleteImage(fileUrlOrPublicId);
    }

    // --- helpers ---

    private String detectImageType(byte[] header) {
        for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
            byte[] magic = entry.getValue();
            boolean match = true;
            for (int i = 0; i < magic.length; i++) {
                if (i >= header.length || header[i] != magic[i]) {
                    match = false;
                    break;
                }
            }
            if (match) return entry.getKey();
        }
        return null;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}