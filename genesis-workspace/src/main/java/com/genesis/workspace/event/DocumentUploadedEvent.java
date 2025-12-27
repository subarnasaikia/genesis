package com.genesis.workspace.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a document is uploaded successfully.
 * Used to trigger async tokenization.
 */
public class DocumentUploadedEvent extends ApplicationEvent {

    private final UUID documentId;
    private final UUID workspaceId;
    private final String storedFileUrl;

    public DocumentUploadedEvent(Object source, UUID documentId, UUID workspaceId, String storedFileUrl) {
        super(source);
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.storedFileUrl = storedFileUrl;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getStoredFileUrl() {
        return storedFileUrl;
    }
}
