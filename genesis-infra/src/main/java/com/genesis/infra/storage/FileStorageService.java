package com.genesis.infra.storage;

import com.genesis.common.exception.GenesisException;
import com.genesis.common.exception.ResourceNotFoundException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * High-level file storage service combining Cloudinary operations with database
 * persistence.
 *
 * <p>
 * This service handles the complete file storage workflow:
 * upload to Cloudinary and save metadata to database.
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final CloudinaryOperations cloudinaryOperations;
    private final StoredFileRepository storedFileRepository;

    public FileStorageService(CloudinaryOperations cloudinaryOperations,
            StoredFileRepository storedFileRepository) {
        this.cloudinaryOperations = cloudinaryOperations;
        this.storedFileRepository = storedFileRepository;
    }

    /**
     * Check if file storage is available (Cloudinary is configured).
     *
     * @return true if file storage is available
     */
    public boolean isAvailable() {
        return cloudinaryOperations.isConfigured();
    }

    /**
     * Store a file to Cloudinary and save metadata to database.
     *
     * @param file   the multipart file to store
     * @param folder optional folder path in Cloudinary
     * @return the stored file entity with URL
     * @throws GenesisException if storage fails
     */
    @Transactional
    public StoredFile store(@NonNull MultipartFile file, String folder) {
        // Upload to Cloudinary
        CloudinaryUploadResult uploadResult = cloudinaryOperations.uploadFile(file, folder);

        // Create and save entity
        StoredFile storedFile = new StoredFile();
        storedFile.setPublicId(uploadResult.getPublicId());
        storedFile.setUrl(uploadResult.getSecureUrl());
        storedFile.setOriginalFilename(uploadResult.getOriginalFilename());
        storedFile.setContentType(file.getContentType());
        storedFile.setFileSize(uploadResult.getBytes());
        storedFile.setFolder(folder);
        storedFile.setResourceType(uploadResult.getResourceType());
        storedFile.setFormat(uploadResult.getFormat());

        StoredFile saved = storedFileRepository.save(storedFile);
        logger.info("Stored file with ID: {} and URL: {}", saved.getId(), saved.getUrl());

        return saved;
    }

    /**
     * Store file content to Cloudinary and save metadata to database.
     *
     * @param data        file content as bytes
     * @param fileName    original file name
     * @param contentType MIME content type
     * @param folder      optional folder path in Cloudinary
     * @return the stored file entity with URL
     * @throws GenesisException if storage fails
     */
    @Transactional
    public StoredFile store(@NonNull byte[] data, @NonNull String fileName,
            String contentType, String folder) {
        // Upload to Cloudinary
        CloudinaryUploadResult uploadResult = cloudinaryOperations.uploadFile(data, fileName, folder);

        // Create and save entity
        StoredFile storedFile = new StoredFile();
        storedFile.setPublicId(uploadResult.getPublicId());
        storedFile.setUrl(uploadResult.getSecureUrl());
        storedFile.setOriginalFilename(fileName);
        storedFile.setContentType(contentType);
        storedFile.setFileSize(uploadResult.getBytes());
        storedFile.setFolder(folder);
        storedFile.setResourceType(uploadResult.getResourceType());
        storedFile.setFormat(uploadResult.getFormat());

        StoredFile saved = storedFileRepository.save(storedFile);
        logger.info("Stored file with ID: {} and URL: {}", saved.getId(), saved.getUrl());

        return saved;
    }

    /**
     * Get a stored file by its ID.
     *
     * @param fileId the file ID
     * @return the stored file entity
     * @throws ResourceNotFoundException if file not found
     */
    public StoredFile getFile(@NonNull UUID fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("StoredFile", fileId));
    }

    /**
     * Get the URL for a stored file.
     *
     * @param fileId the file ID
     * @return the secure URL
     * @throws ResourceNotFoundException if file not found
     */
    public String getFileUrl(@NonNull UUID fileId) {
        StoredFile file = getFile(fileId);
        return file.getUrl();
    }

    /**
     * Delete a file from Cloudinary and database.
     *
     * @param fileId the file ID
     * @throws ResourceNotFoundException if file not found
     * @throws GenesisException          if deletion fails
     */
    @Transactional
    public void delete(@NonNull UUID fileId) {
        StoredFile file = getFile(fileId);

        // Delete from Cloudinary
        cloudinaryOperations.deleteFile(file.getPublicId());

        // Delete from database
        storedFileRepository.delete(file);
        logger.info("Deleted file with ID: {}", fileId);
    }

    /**
     * Delete a file by its Cloudinary public ID.
     *
     * @param publicId the Cloudinary public ID
     * @throws ResourceNotFoundException if file not found
     * @throws GenesisException          if deletion fails
     */
    @Transactional
    public void deleteByPublicId(@NonNull String publicId) {
        StoredFile file = storedFileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("StoredFile", publicId));

        // Delete from Cloudinary
        cloudinaryOperations.deleteFile(publicId);

        // Delete from database
        storedFileRepository.delete(file);
        logger.info("Deleted file with public ID: {}", publicId);
    }
}
