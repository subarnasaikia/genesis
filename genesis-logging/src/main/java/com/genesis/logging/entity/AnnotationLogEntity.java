package com.genesis.logging.entity;

import com.genesis.common.entity.BaseEntity;
import com.genesis.common.event.ActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit row persisted by {@code AnnotationAuditListener} after the
 * source annotation transaction commits. Used for inter-annotator
 * agreement (IAA) metrics and admin observability of annotation activity.
 */
@Entity
@Table(name = "annotation_log", indexes = {
        @Index(name = "idx_log_workspace_user", columnList = "workspace_id, user_id"),
        @Index(name = "idx_log_workspace_timestamp", columnList = "workspace_id, timestamp"),
        @Index(name = "idx_log_action_type", columnList = "action_type")
})
public class AnnotationLogEntity extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * When the action happened — derived from the source event's published
     * timestamp, not the audit-row insert time. Stays meaningful even if
     * the audit listener runs slightly after commit.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Override
    public UUID getId() {
        return super.getId();
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
