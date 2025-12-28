package com.genesis.workspace.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when a member is removed from a workspace.
 */
public class MemberRemovedEvent extends ApplicationEvent {
    private final UUID workspaceId;
    private final String workspaceName;
    private final UUID removedMemberId;
    private final UUID removedByMemberId;

    public MemberRemovedEvent(Object source, UUID workspaceId, String workspaceName,
            UUID removedMemberId, UUID removedByMemberId) {
        super(source);
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.removedMemberId = removedMemberId;
        this.removedByMemberId = removedByMemberId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public UUID getRemovedMemberId() {
        return removedMemberId;
    }

    public UUID getRemovedByMemberId() {
        return removedByMemberId;
    }
}
