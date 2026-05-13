package com.genesis.recommend.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Records that {@code userId} has dismissed (or marked helpful) the
 * recommendation identified by {@code recommendationHash}.
 *
 * <p>Dismiss is idempotent: the unique index on
 * {@code (user_id, recommendation_hash)} guarantees a single row per
 * (user, recommendation) pair. Re-dismissing updates the row in place.
 */
@Entity
@Table(name = "dismissed_recommendations", indexes = {
        @Index(name = "idx_dismissed_user_hash", columnList = "user_id, recommendation_hash", unique = true),
        @Index(name = "idx_dismissed_workspace", columnList = "workspace_id")
})
public class DismissedRecommendationEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Workspace context for analytics. Not part of the unique key. */
    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "recommendation_hash", nullable = false, length = 64)
    private String recommendationHash;

    @Column(name = "dismissed_at", nullable = false)
    private Instant dismissedAt;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Override
    public UUID getId() {
        return super.getId();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getRecommendationHash() { return recommendationHash; }
    public void setRecommendationHash(String recommendationHash) { this.recommendationHash = recommendationHash; }

    public Instant getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(Instant dismissedAt) { this.dismissedAt = dismissedAt; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
