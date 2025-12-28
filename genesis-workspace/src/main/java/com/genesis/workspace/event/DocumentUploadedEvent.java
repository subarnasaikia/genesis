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
    private final UUID userId;
    private final String fileName;

    public DocumentUploadedEvent(Object source, UUID documentId, UUID workspaceId, String storedFileUrl, UUID userId,
            String fileName) {
        super(source);
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.storedFileUrl = storedFileUrl;
        this.userId = userId;
        this.fileName = fileName;
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

    public UUID getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }
}
