package com.genesis.workspace.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Published when asynchronous tokenization fails for a document. Lets {@code
 * genesis-workspace} move its own {@code Document} to {@code FAILED} (and record
 * the error) without {@code genesis-import-export} reaching into the workspace
 * repository (ARCHITECTURE_AUDIT A-006).
 *
 * <p>{@code errorMessage} is already truncated by the publisher to fit the
 * {@code processing_error} column.
 */
public class DocumentProcessingFailedEvent extends ApplicationEvent {

    private final UUID documentId;
    private final String errorMessage;

    public DocumentProcessingFailedEvent(Object source, UUID documentId, String errorMessage) {
        super(source);
        this.documentId = documentId;
        this.errorMessage = errorMessage;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
