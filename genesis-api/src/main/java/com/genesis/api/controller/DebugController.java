package com.genesis.api.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.genesis.common.response.ApiResponse;
import com.genesis.infra.storage.CloudinaryProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug controller for testing configurations.
 * Remove in production!
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final CloudinaryProperties cloudinaryProperties;

    public DebugController(CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @GetMapping("/cloudinary-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCloudinaryConfig() {
        Map<String, Object> info = new HashMap<>();

        String cloudName = cloudinaryProperties.getCloudName();
        String apiKey = cloudinaryProperties.getApiKey();
        String apiSecret = cloudinaryProperties.getApiSecret();

        info.put("cloud_name", cloudName);
        info.put("api_key_length", apiKey != null ? apiKey.length() : 0);
        info.put("api_key_first4", apiKey != null && apiKey.length() >= 4 ? apiKey.substring(0, 4) : "N/A");
        info.put("api_key_last2",
                apiKey != null && apiKey.length() >= 2 ? apiKey.substring(apiKey.length() - 2) : "N/A");
        info.put("api_key_is_numeric", apiKey != null && apiKey.matches("\\d+"));
        info.put("api_secret_length", apiSecret != null ? apiSecret.length() : 0);
        info.put("api_secret_first4", apiSecret != null && apiSecret.length() >= 4 ? apiSecret.substring(0, 4) : "N/A");
        info.put("api_secret_is_numeric", apiSecret != null && apiSecret.matches("\\d+"));
        info.put("is_configured", cloudinaryProperties.isConfigured());

        // Check for common issues
        if (apiKey != null && apiSecret != null) {
            if (apiKey.equals(apiSecret)) {
                info.put("WARNING", "API Key and Secret are IDENTICAL - this is wrong!");
            }
            if (!apiKey.matches("\\d+")) {
                info.put("WARNING", "API Key should be numeric, but contains non-numeric characters");
            }
            if (apiSecret.matches("\\d+")) {
                info.put("WARNING", "API Secret is numeric - this might mean key/secret are swapped!");
            }
        }

        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @GetMapping("/cloudinary-test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCloudinaryConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudinaryProperties.getCloudName(),
                    "api_key", cloudinaryProperties.getApiKey(),
                    "api_secret", cloudinaryProperties.getApiSecret(),
                    "secure", true));

            // Try to ping Cloudinary by fetching account usage
            @SuppressWarnings("unchecked")
            Map<String, Object> usageResult = cloudinary.api().usage(ObjectUtils.emptyMap());

            result.put("status", "SUCCESS");
            result.put("plan", usageResult.get("plan"));
            result.put("credits_usage", usageResult.get("credits"));

        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error_type", e.getClass().getSimpleName());
            result.put("error_message", e.getMessage());

            // Parse the error for more details
            if (e.getMessage() != null && e.getMessage().contains("Unknown API key")) {
                result.put("diagnosis",
                        "The API key is not recognized by Cloudinary. Please verify: 1) The key matches your Cloudinary dashboard, 2) Key and Secret are not swapped, 3) Your Cloudinary account is active");
            }
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
