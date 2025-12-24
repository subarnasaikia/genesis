package com.genesis.infra.storage;

/**
 * DTO containing the result of a Cloudinary upload operation.
 */
public class CloudinaryUploadResult {

    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private long bytes;
    private String resourceType;
    private String originalFilename;

    // Default constructor
    public CloudinaryUploadResult() {
    }

    // All-args constructor
    public CloudinaryUploadResult(String publicId, String url, String secureUrl,
            String format, long bytes, String resourceType, String originalFilename) {
        this.publicId = publicId;
        this.url = url;
        this.secureUrl = secureUrl;
        this.format = format;
        this.bytes = bytes;
        this.resourceType = resourceType;
        this.originalFilename = originalFilename;
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

    public String getSecureUrl() {
        return secureUrl;
    }

    public void setSecureUrl(String secureUrl) {
        this.secureUrl = secureUrl;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    @Override
    public String toString() {
        return "CloudinaryUploadResult{" +
                "publicId='" + publicId + '\'' +
                ", secureUrl='" + secureUrl + '\'' +
                ", format='" + format + '\'' +
                ", bytes=" + bytes +
                ", resourceType='" + resourceType + '\'' +
                '}';
    }
}
