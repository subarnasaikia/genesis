package com.genesis.infra.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for Cloudinary file operations.
 *
 * <p>
 * Provides methods to upload, delete, and retrieve files from Cloudinary.
 */
public interface CloudinaryOperations {

    /**
     * Check if Cloudinary is configured and ready for use.
     *
     * @return true if Cloudinary is configured
     */
    boolean isConfigured();

    /**
     * Upload a file to Cloudinary.
     *
     * @param fileData the file content as bytes
     * @param fileName original file name (used for public_id)
     * @param folder   optional folder path in Cloudinary
     * @return upload result containing URL and metadata
     */
    CloudinaryUploadResult uploadFile(byte[] fileData, String fileName, String folder);

    /**
     * Upload a MultipartFile to Cloudinary.
     *
     * @param file   the multipart file to upload
     * @param folder optional folder path in Cloudinary
     * @return upload result containing URL and metadata
     */
    CloudinaryUploadResult uploadFile(MultipartFile file, String folder);

    /**
     * Delete a file from Cloudinary.
     *
     * @param publicId the Cloudinary public ID of the file
     * @return true if deletion was successful
     */
    boolean deleteFile(String publicId);

    /**
     * Get the secure URL for a file by its public ID.
     *
     * @param publicId the Cloudinary public ID
     * @return the secure URL
     */
    String getFileUrl(String publicId);
}
