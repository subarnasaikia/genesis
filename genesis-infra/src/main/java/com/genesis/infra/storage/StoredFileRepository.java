package com.genesis.infra.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for StoredFile entities.
 */
@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    /**
     * Find a stored file by its Cloudinary public ID.
     *
     * @param publicId the Cloudinary public ID
     * @return optional containing the file if found
     */
    Optional<StoredFile> findByPublicId(String publicId);

    /**
     * Find all stored files in a specific folder.
     *
     * @param folder the folder path
     * @return list of stored files in the folder
     */
    List<StoredFile> findByFolder(String folder);

    /**
     * Find all stored files with a specific original filename.
     *
     * @param originalFilename the original filename
     * @return list of stored files with the filename
     */
    List<StoredFile> findByOriginalFilename(String originalFilename);

    /**
     * Check if a file with the given public ID exists.
     *
     * @param publicId the Cloudinary public ID
     * @return true if the file exists
     */
    boolean existsByPublicId(String publicId);
}
