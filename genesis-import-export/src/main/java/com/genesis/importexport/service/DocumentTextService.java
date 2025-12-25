package com.genesis.importexport.service;

import com.genesis.common.exception.GenesisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service for reading text content from uploaded files and URLs.
 *
 * <p>
 * Handles UTF-8 encoding for Assamese and other Unicode text.
 */
@Service
public class DocumentTextService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTextService.class);

    /**
     * Extract text content from a MultipartFile.
     * Assumes UTF-8 encoding.
     *
     * @param file the uploaded file
     * @return the text content
     * @throws GenesisException if reading fails
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GenesisException("File is empty");
        }

        try {
            byte[] bytes = file.getBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to extract text from file: {}", file.getOriginalFilename(), e);
            throw new GenesisException("Failed to read file content: " + e.getMessage());
        }
    }

    /**
     * Download and extract text content from a URL.
     * Assumes UTF-8 encoding.
     *
     * @param fileUrl the file URL
     * @return the text content
     * @throws GenesisException if download/reading fails
     */
    public String extractTextFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new GenesisException("File URL is empty");
        }

        try {
            URL url = new URL(fileUrl);
            StringBuilder content = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            // Remove trailing newline if present
            if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
                content.setLength(content.length() - 1);
            }

            return content.toString();

        } catch (IOException e) {
            logger.error("Failed to download text from URL: {}", fileUrl, e);
            throw new GenesisException("Failed to download file content: " + e.getMessage());
        }
    }

    /**
     * Check if a file is a text file based on its name.
     *
     * @param filename the file name
     * @return true if it's a text file
     */
    public boolean isTextFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".txt") || lowerName.endsWith(".conll");
    }
}
