package com.genesis.infra.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @Mock
    private CloudinaryOperations cloudinaryOperations;

    @Mock
    private StoredFileRepository storedFileRepository;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(cloudinaryOperations, storedFileRepository);
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailable {

        @Test
        @DisplayName("returns true when Cloudinary is configured")
        void returnsTrueWhenConfigured() {
            when(cloudinaryOperations.isConfigured()).thenReturn(true);
            assertTrue(fileStorageService.isAvailable());
        }

        @Test
        @DisplayName("returns false when Cloudinary is not configured")
        void returnsFalseWhenNotConfigured() {
            when(cloudinaryOperations.isConfigured()).thenReturn(false);
            assertFalse(fileStorageService.isAvailable());
        }
    }

    @Nested
    @DisplayName("store(MultipartFile)")
    class StoreMultipartFile {

        @Test
        @DisplayName("uploads file and saves to database")
        void uploadsAndSaves() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());

            CloudinaryUploadResult uploadResult = new CloudinaryUploadResult();
            uploadResult.setPublicId("test-public-id");
            uploadResult.setSecureUrl("https://cloudinary.com/test.txt");
            uploadResult.setOriginalFilename("test.txt");
            uploadResult.setBytes(7L);
            uploadResult.setResourceType("raw");
            uploadResult.setFormat("txt");

            when(cloudinaryOperations.uploadFile(eq(file), eq("imports")))
                    .thenReturn(uploadResult);
            when(storedFileRepository.save(any(StoredFile.class)))
                    .thenAnswer(inv -> {
                        StoredFile sf = inv.getArgument(0);
                        sf.setId(UUID.randomUUID());
                        return sf;
                    });

            // Act
            StoredFile result = fileStorageService.store(file, "imports");

            // Assert
            assertNotNull(result);
            assertEquals("test-public-id", result.getPublicId());
            assertEquals("https://cloudinary.com/test.txt", result.getUrl());
            assertEquals("test.txt", result.getOriginalFilename());
            assertEquals("text/plain", result.getContentType());
            assertEquals("imports", result.getFolder());

            verify(cloudinaryOperations).uploadFile(file, "imports");
            verify(storedFileRepository).save(any(StoredFile.class));
        }
    }

    @Nested
    @DisplayName("store(byte[])")
    class StoreBytes {

        @Test
        @DisplayName("uploads bytes and saves to database")
        void uploadsAndSaves() {
            // Arrange
            byte[] data = "test content".getBytes();

            CloudinaryUploadResult uploadResult = new CloudinaryUploadResult();
            uploadResult.setPublicId("bytes-public-id");
            uploadResult.setSecureUrl("https://cloudinary.com/file.txt");
            uploadResult.setOriginalFilename("file.txt");
            uploadResult.setBytes(12L);
            uploadResult.setResourceType("raw");

            when(cloudinaryOperations.uploadFile(eq(data), eq("file.txt"), eq("exports")))
                    .thenReturn(uploadResult);
            when(storedFileRepository.save(any(StoredFile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            StoredFile result = fileStorageService.store(data, "file.txt", "text/plain", "exports");

            // Assert
            assertEquals("bytes-public-id", result.getPublicId());
            assertEquals("text/plain", result.getContentType());
            assertEquals("exports", result.getFolder());
        }
    }

    @Nested
    @DisplayName("getFile")
    class GetFile {

        @Test
        @DisplayName("returns file when found")
        void returnsFileWhenFound() {
            UUID id = UUID.randomUUID();
            StoredFile expected = new StoredFile();
            expected.setId(id);
            expected.setPublicId("test-id");

            when(storedFileRepository.findById(id)).thenReturn(Optional.of(expected));

            StoredFile result = fileStorageService.getFile(id);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(storedFileRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> fileStorageService.getFile(id));
        }
    }

    @Nested
    @DisplayName("getFileUrl")
    class GetFileUrl {

        @Test
        @DisplayName("returns URL for existing file")
        void returnsUrlForExistingFile() {
            UUID id = UUID.randomUUID();
            StoredFile file = new StoredFile();
            file.setUrl("https://cloudinary.com/image.jpg");

            when(storedFileRepository.findById(id)).thenReturn(Optional.of(file));

            String url = fileStorageService.getFileUrl(id);

            assertEquals("https://cloudinary.com/image.jpg", url);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes from Cloudinary and database")
        void deletesFromBoth() {
            UUID id = UUID.randomUUID();
            StoredFile file = new StoredFile();
            file.setId(id);
            file.setPublicId("delete-me");

            when(storedFileRepository.findById(id)).thenReturn(Optional.of(file));
            when(cloudinaryOperations.deleteFile("delete-me")).thenReturn(true);

            fileStorageService.delete(id);

            verify(cloudinaryOperations).deleteFile("delete-me");
            verify(storedFileRepository).delete(file);
        }

        @Test
        @DisplayName("throws when file not found")
        void throwsWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(storedFileRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> fileStorageService.delete(id));

            verify(cloudinaryOperations, never()).deleteFile(any());
            verify(storedFileRepository, never()).delete(any(StoredFile.class));
        }
    }

    @Nested
    @DisplayName("deleteByPublicId")
    class DeleteByPublicId {

        @Test
        @DisplayName("deletes by public ID")
        void deletesByPublicId() {
            StoredFile file = new StoredFile();
            file.setPublicId("my-public-id");

            when(storedFileRepository.findByPublicId("my-public-id"))
                    .thenReturn(Optional.of(file));
            when(cloudinaryOperations.deleteFile("my-public-id")).thenReturn(true);

            fileStorageService.deleteByPublicId("my-public-id");

            verify(cloudinaryOperations).deleteFile("my-public-id");
            verify(storedFileRepository).delete(file);
        }
    }
}
