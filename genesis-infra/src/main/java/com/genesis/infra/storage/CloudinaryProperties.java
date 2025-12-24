package com.genesis.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Cloudinary file storage service.
 *
 * <p>
 * Properties are prefixed with "cloudinary" and loaded from
 * application.properties
 * or environment variables.
 */
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    /**
     * Cloudinary cloud name (required).
     */
    private String cloudName;

    /**
     * Cloudinary API key (required).
     */
    private String apiKey;

    /**
     * Cloudinary API secret (required).
     */
    private String apiSecret;

    // Getters and Setters

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    /**
     * Check if Cloudinary is configured with required properties.
     *
     * @return true if all required properties are set
     */
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }
}
