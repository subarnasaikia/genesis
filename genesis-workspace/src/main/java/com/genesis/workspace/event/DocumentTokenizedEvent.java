package com.genesis.workspace.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

public class DocumentTokenizedEvent extends ApplicationEvent {
    private final UUID documentId;
    private final UUID workspaceId;
    private final String documentName;

    public DocumentTokenizedEvent(Object source, UUID documentId, UUID workspaceId, String documentName) {
        super(source);
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.documentName = documentName;
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
}
