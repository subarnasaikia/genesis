package com.genesis.infra.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.genesis.common.exception.GenesisException;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling Cloudinary file operations.
 *
 * <p>
 * Provides methods to upload, delete, and retrieve files from Cloudinary.
 */
@Service
public class CloudinaryService implements CloudinaryOperations {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryService(CloudinaryProperties properties) {
        if (properties.isConfigured()) {
            // Log loaded credentials (masked for security)
            String maskedKey = maskCredential(properties.getApiKey());
            String maskedSecret = maskCredential(properties.getApiSecret());
            logger.info("Cloudinary config - cloud_name: {}, api_key: {}, api_secret: {}",
                    properties.getCloudName(), maskedKey, maskedSecret);

            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", properties.getCloudName(),
                    "api_key", properties.getApiKey(),
                    "api_secret", properties.getApiSecret(),
                    "secure", true));
            this.configured = true;
            logger.info("Cloudinary service initialized successfully");
        } else {
            this.cloudinary = null;
            this.configured = false;
            logger.warn("Cloudinary is not configured. File storage operations will not be available.");
        }
    }

    /**
     * Check if Cloudinary is configured and ready for use.
     *
     * @return true if Cloudinary is configured
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Upload a file to Cloudinary.
     *
     * @param fileData the file content as bytes
     * @param fileName original file name (used for public_id)
     * @param folder   optional folder path in Cloudinary
     * @return upload result containing URL and metadata
     * @throws GenesisException if upload fails or Cloudinary is not configured
     */
    public CloudinaryUploadResult uploadFile(@NonNull byte[] fileData, @NonNull String fileName, String folder) {
        ensureConfigured();

        try {
            // Prepare upload options
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", "auto",
                    "use_filename", true,
                    "unique_filename", true);

            if (folder != null && !folder.isBlank()) {
                options.put("folder", folder);
            }

            // Upload file
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(fileData, options);

            return mapToUploadResult(result, fileName);

        } catch (IOException e) {
            logger.error("Failed to upload file to Cloudinary: {}", fileName, e);
            throw new GenesisException("Failed to upload file to Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Upload a MultipartFile to Cloudinary.
     *
     * @param file   the multipart file to upload
     * @param folder optional folder path in Cloudinary
     * @return upload result containing URL and metadata
     * @throws GenesisException if upload fails or Cloudinary is not configured
     */
    public CloudinaryUploadResult uploadFile(@NonNull MultipartFile file, String folder) {
        ensureConfigured();

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown";
            }

            return uploadFile(file.getBytes(), originalFilename, folder);

        } catch (IOException e) {
            logger.error("Failed to read multipart file for upload", e);
            throw new GenesisException("Failed to read file for upload: " + e.getMessage());
        }
    }

    /**
     * Delete a file from Cloudinary.
     *
     * @param publicId the Cloudinary public ID of the file
     * @return true if deletion was successful
     * @throws GenesisException if deletion fails or Cloudinary is not configured
     */
    public boolean deleteFile(@NonNull String publicId) {
        ensureConfigured();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            String resultStatus = (String) result.get("result");
            boolean deleted = "ok".equals(resultStatus);

            if (deleted) {
                logger.info("Successfully deleted file from Cloudinary: {}", publicId);
            } else {
                logger.warn("File not found in Cloudinary for deletion: {}", publicId);
            }

            return deleted;

        } catch (IOException e) {
            logger.error("Failed to delete file from Cloudinary: {}", publicId, e);
            throw new GenesisException("Failed to delete file from Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Get the secure URL for a file by its public ID.
     *
     * @param publicId the Cloudinary public ID
     * @return the secure URL
     * @throws GenesisException if Cloudinary is not configured
     */
    public String getFileUrl(@NonNull String publicId) {
        ensureConfigured();
        return cloudinary.url().secure(true).generate(publicId);
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new GenesisException(
                    "Cloudinary is not configured. Please set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET environment variables.");
        }
    }

    private CloudinaryUploadResult mapToUploadResult(Map<String, Object> result, String originalFilename) {
        CloudinaryUploadResult uploadResult = new CloudinaryUploadResult();
        uploadResult.setPublicId((String) result.get("public_id"));
        uploadResult.setUrl((String) result.get("url"));
        uploadResult.setSecureUrl((String) result.get("secure_url"));
        uploadResult.setFormat((String) result.get("format"));
        uploadResult.setResourceType((String) result.get("resource_type"));
        uploadResult.setOriginalFilename(originalFilename);

        Object bytesObj = result.get("bytes");
        if (bytesObj instanceof Number) {
            uploadResult.setBytes(((Number) bytesObj).longValue());
        }

        logger.info("Successfully uploaded file to Cloudinary: {}", uploadResult.getPublicId());
        return uploadResult;
    }

    private String maskCredential(String value) {
        if (value == null || value.length() < 6) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 2);
    }
}
