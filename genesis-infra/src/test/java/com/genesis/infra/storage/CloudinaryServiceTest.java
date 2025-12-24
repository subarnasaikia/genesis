package com.genesis.infra.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.common.exception.GenesisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CloudinaryService Tests")
class CloudinaryServiceTest {

    private CloudinaryService cloudinaryService;

    @Nested
    @DisplayName("When Cloudinary is not configured")
    class NotConfigured {

        @BeforeEach
        void setUp() {
            CloudinaryProperties properties = new CloudinaryProperties();
            // Properties are empty - not configured
            cloudinaryService = new CloudinaryService(properties);
        }

        @Test
        @DisplayName("isConfigured should return false")
        void isConfiguredReturnsFalse() {
            assertFalse(cloudinaryService.isConfigured());
        }

        @Test
        @DisplayName("uploadFile should throw exception")
        void uploadFileThrowsException() {
            byte[] data = "test".getBytes();

            GenesisException ex = assertThrows(GenesisException.class,
                    () -> cloudinaryService.uploadFile(data, "test.txt", null));

            assertTrue(ex.getMessage().contains("not configured"));
        }

        @Test
        @DisplayName("deleteFile should throw exception")
        void deleteFileThrowsException() {
            GenesisException ex = assertThrows(GenesisException.class,
                    () -> cloudinaryService.deleteFile("public-id"));

            assertTrue(ex.getMessage().contains("not configured"));
        }

        @Test
        @DisplayName("getFileUrl should throw exception")
        void getFileUrlThrowsException() {
            GenesisException ex = assertThrows(GenesisException.class,
                    () -> cloudinaryService.getFileUrl("public-id"));

            assertTrue(ex.getMessage().contains("not configured"));
        }
    }

    @Nested
    @DisplayName("When Cloudinary is configured")
    class Configured {

        @BeforeEach
        void setUp() {
            CloudinaryProperties properties = new CloudinaryProperties();
            properties.setCloudName("test-cloud");
            properties.setApiKey("test-api-key");
            properties.setApiSecret("test-api-secret");
            cloudinaryService = new CloudinaryService(properties);
        }

        @Test
        @DisplayName("isConfigured should return true")
        void isConfiguredReturnsTrue() {
            assertTrue(cloudinaryService.isConfigured());
        }
    }

    @Nested
    @DisplayName("CloudinaryProperties Tests")
    class PropertiesTests {

        @Test
        @DisplayName("isConfigured returns false when cloudName is null")
        void isConfiguredFalseWhenCloudNameNull() {
            CloudinaryProperties props = new CloudinaryProperties();
            props.setApiKey("key");
            props.setApiSecret("secret");

            assertFalse(props.isConfigured());
        }

        @Test
        @DisplayName("isConfigured returns false when apiKey is blank")
        void isConfiguredFalseWhenApiKeyBlank() {
            CloudinaryProperties props = new CloudinaryProperties();
            props.setCloudName("cloud");
            props.setApiKey("   ");
            props.setApiSecret("secret");

            assertFalse(props.isConfigured());
        }

        @Test
        @DisplayName("isConfigured returns true when all properties set")
        void isConfiguredTrueWhenAllSet() {
            CloudinaryProperties props = new CloudinaryProperties();
            props.setCloudName("cloud");
            props.setApiKey("key");
            props.setApiSecret("secret");

            assertTrue(props.isConfigured());
        }
    }

    @Nested
    @DisplayName("CloudinaryUploadResult Tests")
    class UploadResultTests {

        @Test
        @DisplayName("should store all fields correctly")
        void storesAllFields() {
            CloudinaryUploadResult result = new CloudinaryUploadResult(
                    "public-id-123",
                    "http://example.com/image.jpg",
                    "https://example.com/image.jpg",
                    "jpg",
                    12345L,
                    "image",
                    "original.jpg");

            assertEquals("public-id-123", result.getPublicId());
            assertEquals("http://example.com/image.jpg", result.getUrl());
            assertEquals("https://example.com/image.jpg", result.getSecureUrl());
            assertEquals("jpg", result.getFormat());
            assertEquals(12345L, result.getBytes());
            assertEquals("image", result.getResourceType());
            assertEquals("original.jpg", result.getOriginalFilename());
        }

        @Test
        @DisplayName("toString should contain key info")
        void toStringContainsKeyInfo() {
            CloudinaryUploadResult result = new CloudinaryUploadResult();
            result.setPublicId("test-id");
            result.setSecureUrl("https://test.com");

            String str = result.toString();
            assertTrue(str.contains("test-id"));
            assertTrue(str.contains("https://test.com"));
        }
    }
}
