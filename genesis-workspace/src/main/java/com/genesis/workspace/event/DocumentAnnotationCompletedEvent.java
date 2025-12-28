package com.genesis.workspace.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when a document's annotation status is marked as COMPLETED.
 */
public class DocumentAnnotationCompletedEvent extends ApplicationEvent {
    private final UUID documentId;
    private final UUID workspaceId;
    private final String documentName;
    private final UUID completedByUserId;

    public DocumentAnnotationCompletedEvent(Object source, UUID documentId, UUID workspaceId,
            String documentName, UUID completedByUserId) {
        super(source);
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.documentName = documentName;
        this.completedByUserId = completedByUserId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public UUID getCompletedByUserId() {
        return completedByUserId;
    }
}
