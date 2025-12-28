package com.genesis.workspace.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;
import java.util.List;

public class WorkspaceDeletedEvent extends ApplicationEvent {
    private final UUID workspaceId;
    private final String workspaceName;
    private final List<UUID> memberIds; // To notify them

    public WorkspaceDeletedEvent(Object source, UUID workspaceId, String workspaceName, List<UUID> memberIds) {
        super(source);
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.memberIds = memberIds;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public List<UUID> getMemberIds() {
        return memberIds;
    }
}
