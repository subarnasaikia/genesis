package com.genesis.common.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Published by annotation-mutating services. Persisted by
 * AnnotationAuditListener (genesis-logging) on AFTER_COMMIT — so a
 * failed audit write does not roll back the annotation itself.
 *
 * <p>{@code entityId} is the primary id of the entity acted on
 * (mention id, cluster id, token id). {@code payloadJson} captures
 * action-specific diff data and may be null when the action carries no
 * structured payload.
 */
public class AnnotationLogEvent extends ApplicationEvent {

    private final UUID workspaceId;
    private final String userId;
    private final ActionType actionType;
    private final UUID entityId;
    private final String payloadJson;

    public AnnotationLogEvent(Object source,
            UUID workspaceId,
            String userId,
            ActionType actionType,
            UUID entityId,
            String payloadJson) {
        super(source);
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.actionType = actionType;
        this.entityId = entityId;
        this.payloadJson = payloadJson;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
