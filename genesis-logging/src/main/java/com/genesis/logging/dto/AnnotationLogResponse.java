package com.genesis.logging.dto;

import com.genesis.common.event.ActionType;
import com.genesis.logging.entity.AnnotationLogEntity;
import java.time.Instant;
import java.util.UUID;

public class AnnotationLogResponse {

    private UUID id;
    private UUID workspaceId;
    private String userId;
    private ActionType actionType;
    private UUID entityId;
    private Instant timestamp;
    private String payloadJson;

    public static AnnotationLogResponse from(AnnotationLogEntity e) {
        AnnotationLogResponse r = new AnnotationLogResponse();
        r.id = e.getId();
        r.workspaceId = e.getWorkspaceId();
        r.userId = e.getUserId();
        r.actionType = e.getActionType();
        r.entityId = e.getEntityId();
        r.timestamp = e.getTimestamp();
        r.payloadJson = e.getPayloadJson();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getUserId() { return userId; }
    public ActionType getActionType() { return actionType; }
    public UUID getEntityId() { return entityId; }
    public Instant getTimestamp() { return timestamp; }
    public String getPayloadJson() { return payloadJson; }
}
