package com.genesis.infra.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage backend abstraction — the seam that lets a deployment choose where raw
 * uploaded files live (e.g. Cloudinary or the local filesystem).
 *
 * <p>
 * Implementations are selected per deployment via
 * {@code genesis.storage.provider} (see {@link StorageProperties}). Exactly one
 * backend is active at runtime; {@link FileStorageService} is wired against this
 * interface and never knows which one it got.
 *
 * <p>
 * The {@code reference} passed to {@link #downloadAsString(String)} is whatever
 * was stored as the file's URL at upload time — an {@code https://…} URL for the
 * Cloudinary backend, a {@code local:…} key for the local-disk backend. Because
 * only one backend is active, the reference scheme always matches the backend
 * that produced it.
 */
public interface FileStorageBackend {

    /**
     * Whether this backend is ready to serve upload/download/delete calls.
     *
     * @return true if the backend is usable
     */
    boolean isAvailable();

    /**
     * Store file content and return its public reference + metadata.
     *
     * @param fileData the file content as bytes
     * @param fileName original file name
     * @param folder   optional logical folder/prefix
     * @return upload result containing the stored reference and metadata
     */
    CloudinaryUploadResult uploadFile(byte[] fileData, String fileName, String folder);

    /**
     * Store a multipart file and return its public reference + metadata.
     *
     * @param file   the multipart file to store
     * @param folder optional logical folder/prefix
     * @return upload result containing the stored reference and metadata
     */
    CloudinaryUploadResult uploadFile(MultipartFile file, String folder);

    /**
     * Read previously stored content back as a UTF-8 string.
     *
     * @param reference the stored reference (URL or storage key) persisted as the
     *                  file's URL at upload time
     * @return the file content as a string
     */
    String downloadAsString(String reference);

    /**
     * Remove a stored object by its storage key (the value persisted as
     * {@code publicId}).
     *
     * @param storageKey the backend-specific storage key
     * @return true if the object was removed
     */
    boolean deleteFile(String storageKey);
}
