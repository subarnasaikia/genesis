package com.genesis.infra.storage;

import com.genesis.common.exception.GenesisException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local-filesystem {@link FileStorageBackend}.
 *
 * <p>
 * Active when {@code genesis.storage.provider=local}. Files are written under
 * {@link StorageProperties.Local#getBasePath()} as
 * {@code {folder}/{uuid}-{safeName}}. The storage key (relative path) is stored
 * as the file's {@code publicId}; the file's URL is the same key prefixed with
 * {@code local:} so server-side reads resolve back to disk.
 *
 * <p>
 * <b>Deployment note:</b> the base directory must live on durable, single-writer
 * storage. On ephemeral containers it is wiped on redeploy and is not shared
 * across instances — mount a persistent volume and run a single instance.
 */
@Service
@ConditionalOnProperty(name = "genesis.storage.provider", havingValue = "local")
public class LocalDiskStorageBackend implements FileStorageBackend {

    private static final Logger logger = LoggerFactory.getLogger(LocalDiskStorageBackend.class);

    /** URL scheme stored on {@code StoredFile.url} so downloads resolve to disk. */
    static final String LOCAL_SCHEME = "local:";

    private final Path baseDir;

    public LocalDiskStorageBackend(StorageProperties properties) {
        this.baseDir = Paths.get(properties.getLocal().getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            logger.info("Local file storage initialized at {}", baseDir);
        } catch (IOException e) {
            throw new GenesisException(
                    "Failed to create local storage directory " + baseDir + ": " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return Files.isDirectory(baseDir) && Files.isWritable(baseDir);
    }

    @Override
    public CloudinaryUploadResult uploadFile(@NonNull byte[] fileData, @NonNull String fileName, String folder) {
        String storageKey = buildStorageKey(fileName, folder);
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, fileData);
        } catch (IOException e) {
            throw new GenesisException("Failed to write file to local storage: " + e.getMessage());
        }
        logger.info("Stored file on local disk: {}", storageKey);
        return toResult(storageKey, fileName, fileData.length);
    }

    @Override
    public CloudinaryUploadResult uploadFile(@NonNull MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "unknown";
            }
            return uploadFile(file.getBytes(), originalFilename, folder);
        } catch (IOException e) {
            throw new GenesisException("Failed to read file for upload: " + e.getMessage());
        }
    }

    @Override
    public String downloadAsString(@NonNull String reference) {
        Path source = resolve(stripScheme(reference));
        try {
            return Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GenesisException("Failed to read file from local storage: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(@NonNull String storageKey) {
        Path target = resolve(stripScheme(storageKey));
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                logger.info("Deleted file from local storage: {}", storageKey);
            } else {
                logger.warn("File not found in local storage for deletion: {}", storageKey);
            }
            return deleted;
        } catch (IOException e) {
            throw new GenesisException("Failed to delete file from local storage: " + e.getMessage());
        }
    }

    private CloudinaryUploadResult toResult(String storageKey, String fileName, long bytes) {
        CloudinaryUploadResult result = new CloudinaryUploadResult();
        result.setPublicId(storageKey);
        result.setUrl(LOCAL_SCHEME + storageKey);
        result.setSecureUrl(LOCAL_SCHEME + storageKey);
        result.setOriginalFilename(fileName);
        result.setBytes(bytes);
        result.setResourceType("raw");
        result.setFormat(extension(fileName));
        return result;
    }

    private String buildStorageKey(String fileName, String folder) {
        String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        String unique = UUID.randomUUID() + "-" + safeName;
        if (folder == null || folder.isBlank()) {
            return unique;
        }
        String cleanFolder = folder.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        return cleanFolder.isEmpty() ? unique : cleanFolder + "/" + unique;
    }

    /**
     * Resolve a storage key under the base directory, rejecting any key that
     * escapes it (path-traversal guard).
     */
    private Path resolve(String storageKey) {
        Path resolved = baseDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new GenesisException("Resolved storage path escapes the base directory: " + storageKey);
        }
        return resolved;
    }

    private static String stripScheme(String reference) {
        return reference.startsWith(LOCAL_SCHEME) ? reference.substring(LOCAL_SCHEME.length()) : reference;
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1
                ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT)
                : "";
    }
}
