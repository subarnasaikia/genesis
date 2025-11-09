package com.genesis.common.interfaces;

import com.genesis.common.model.DocumentText;

/**
 * Core interface for text processing operations.
 */
public interface TextProcessor {
    /**
     * Process raw text into structured document text.
     *
     * @param rawText The input text to process
     * @return Processed document text with structural information
     */
    DocumentText process(String rawText);
}