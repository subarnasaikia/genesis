package com.genesis.workspace.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

public class MemberAddedEvent extends ApplicationEvent {
    private final UUID workspaceId;
    private final String workspaceName;
    private final UUID addedMemberId;
    private final UUID addedByMemberId;

    public MemberAddedEvent(Object source, UUID workspaceId, String workspaceName, UUID addedMemberId,
            UUID addedByMemberId) {
        super(source);
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.addedMemberId = addedMemberId;
        this.addedByMemberId = addedByMemberId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public UUID getAddedMemberId() {
        return addedMemberId;
    }

    public UUID getAddedByMemberId() {
        return addedByMemberId;
    }
}
