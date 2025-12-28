package com.genesis.common.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when any activity occurs in a workspace.
 * Used to update the workspace's last modified timestamp.
 */
public class WorkspaceActivityEvent extends ApplicationEvent {

    private final UUID workspaceId;

    public WorkspaceActivityEvent(Object source, UUID workspaceId) {
        super(source);
        this.workspaceId = workspaceId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }
}
