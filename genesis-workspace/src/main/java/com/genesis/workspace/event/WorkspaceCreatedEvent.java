package com.genesis.workspace.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

public class WorkspaceCreatedEvent extends ApplicationEvent {
    private final UUID workspaceId;
    private final String workspaceName;
    private final UUID ownerId;

    public WorkspaceCreatedEvent(Object source, UUID workspaceId, String workspaceName, UUID ownerId) {
        super(source);
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.ownerId = ownerId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }
}
