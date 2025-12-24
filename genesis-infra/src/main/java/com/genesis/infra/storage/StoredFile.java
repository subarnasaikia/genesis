package com.genesis.infra.storage;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity representing a file stored in Cloudinary.
 *
 * <p>
 * Tracks uploaded files with their Cloudinary metadata and URLs
 * for persistent reference in the database.
 */
@Entity
@Table(name = "stored_files")
public class StoredFile extends BaseEntity {

    /**
     * Cloudinary public ID for the file.
     */
    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    /**
     * Secure URL to access the file.
     */
    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    /**
     * Original filename as uploaded by the user.
     */
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /**
     * MIME content type of the file.
     */
    @Column(name = "content_type")
    private String contentType;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Cloudinary folder path where the file is stored.
     */
    @Column(name = "folder")
    private String folder;

    /**
     * Cloudinary resource type (image, raw, video, auto).
     */
    @Column(name = "resource_type")
    private String resourceType;

    /**
     * File format/extension.
     */
    @Column(name = "format")
    private String format;

    // Default constructor
    public StoredFile() {
    }

    // Getters and Setters

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
