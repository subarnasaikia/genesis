package com.genesis.infra.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.common.exception.GenesisException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("LocalDiskStorageBackend Tests")
class LocalDiskStorageBackendTest {

    @TempDir
    Path tempDir;

    private LocalDiskStorageBackend backend;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider(StorageProperties.PROVIDER_LOCAL);
        properties.getLocal().setBasePath(tempDir.toString());
        backend = new LocalDiskStorageBackend(properties);
    }

    @Test
    @DisplayName("isAvailable returns true for a writable base directory")
    void isAvailableTrue() {
        assertTrue(backend.isAvailable());
    }

    @Test
    @DisplayName("uploadFile(byte[]) writes to disk and is readable back via the stored URL")
    void uploadBytesRoundTrip() {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

        CloudinaryUploadResult result = backend.uploadFile(data, "sample.txt", "workspaces/ws-1/documents");

        assertNotNull(result.getPublicId());
        assertTrue(result.getPublicId().startsWith("workspaces/ws-1/documents/"));
        assertTrue(result.getPublicId().endsWith("-sample.txt"));
        assertEquals(LocalDiskStorageBackend.LOCAL_SCHEME + result.getPublicId(), result.getUrl());
        assertEquals(result.getUrl(), result.getSecureUrl());
        assertEquals("raw", result.getResourceType());
        assertEquals("txt", result.getFormat());
        assertEquals(data.length, result.getBytes());

        // File physically exists under the base dir, and downloads return its content.
        assertTrue(Files.exists(tempDir.resolve(result.getPublicId())));
        assertEquals("hello world", backend.downloadAsString(result.getUrl()));
    }

    @Test
    @DisplayName("uploadFile(MultipartFile) writes to disk and is readable back")
    void uploadMultipartRoundTrip() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.conll", "text/plain", "#begin document".getBytes(StandardCharsets.UTF_8));

        CloudinaryUploadResult result = backend.uploadFile(file, "imports");

        assertEquals("doc.conll", result.getOriginalFilename());
        assertEquals("#begin document", backend.downloadAsString(result.getUrl()));
    }

    @Test
    @DisplayName("uploadFile with no folder stores at the base directory root")
    void uploadWithoutFolder() {
        CloudinaryUploadResult result = backend.uploadFile("x".getBytes(StandardCharsets.UTF_8), "a.txt", null);

        assertFalse(result.getPublicId().contains("/"));
        assertEquals("x", backend.downloadAsString(result.getUrl()));
    }

    @Test
    @DisplayName("deleteFile removes the stored file")
    void deleteRemovesFile() {
        CloudinaryUploadResult result = backend.uploadFile("bye".getBytes(StandardCharsets.UTF_8), "z.txt", "f");
        assertTrue(Files.exists(tempDir.resolve(result.getPublicId())));

        boolean deleted = backend.deleteFile(result.getPublicId());

        assertTrue(deleted);
        assertFalse(Files.exists(tempDir.resolve(result.getPublicId())));
    }

    @Test
    @DisplayName("deleteFile returns false when the file is absent")
    void deleteMissingReturnsFalse() {
        assertFalse(backend.deleteFile("does/not/exist.txt"));
    }

    @Test
    @DisplayName("downloadAsString throws for a missing file")
    void downloadMissingThrows() {
        assertThrows(GenesisException.class,
                () -> backend.downloadAsString(LocalDiskStorageBackend.LOCAL_SCHEME + "missing.txt"));
    }

    @Test
    @DisplayName("path traversal outside the base directory is rejected")
    void rejectsPathTraversal() {
        assertThrows(GenesisException.class,
                () -> backend.downloadAsString(LocalDiskStorageBackend.LOCAL_SCHEME + "../escape.txt"));
        assertThrows(GenesisException.class,
                () -> backend.deleteFile("../escape.txt"));
    }
}
