package com.cou.bustracker.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    /**
     * Uploads an image to Cloudinary and returns its secure HTTPS URL.
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        ensureConfigured();
        Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image"
        ));
        return (String) result.get("secure_url");
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
                    "resource_type", "image"
            ));
            log.debug("Cloudinary delete result for '{}': {}", publicId, result);
        } catch (Exception e) {
            log.warn("Failed to delete image '{}' from Cloudinary: {}", imageUrlOrPublicId, e.getMessage());
        }
    }

    private void ensureConfigured() {
        if (cloudName == null || cloudName.isBlank()) {
            throw new IllegalStateException(
                    "Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY " +
                            "and CLOUDINARY_API_SECRET before uploading images."
            );
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