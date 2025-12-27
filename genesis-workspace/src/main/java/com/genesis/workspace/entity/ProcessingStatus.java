package com.genesis.workspace.entity;

/**
 * Status of document processing (tokenization).
 */
public enum ProcessingStatus {
    /**
     * Document uploaded, waiting for tokenization.
     */
    PENDING,

    /**
     * Tokenization in progress.
     */
    PROCESSING,

    /**
     * Tokenization completed successfully.
     */
    COMPLETED,

    /**
     * Tokenization failed.
     */
    FAILED
}
