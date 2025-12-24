package com.genesis.workspace.entity;

/**
 * Document lifecycle status.
 */
public enum DocumentStatus {
    /**
     * Document is uploaded but not yet imported/processed.
     */
    UPLOADED,

    /**
     * Document has been imported and tokenized.
     */
    IMPORTED,

    /**
     * Document is currently being annotated.
     */
    ANNOTATING,

    /**
     * Document annotation is complete.
     */
    COMPLETE
}
