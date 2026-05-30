package com.genesis.workspace.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Published when asynchronous tokenization begins for a document. Lets {@code
 * genesis-workspace} move its own {@code Document} to {@code PROCESSING} without
 * {@code genesis-import-export} reaching into the workspace repository
 * (ARCHITECTURE_AUDIT A-006).
 */
public class DocumentProcessingStartedEvent extends ApplicationEvent {

    private final UUID documentId;

    public DocumentProcessingStartedEvent(Object source, UUID documentId) {
        super(source);
        this.documentId = documentId;
    }

    public UUID getDocumentId() {
        return documentId;
    }
}
