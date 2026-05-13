package com.genesis.wsd.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Per-annotator word-sense assignment for a token.
 *
 * <p>One row per (token, annotator). Multiple annotators may pick different
 * senses for the same token; the consensus is computed at export time.
 * {@code senseId} is a logical FK to {@code wsd_sense.id} but is stored as a
 * plain UUID column (no DB constraint) — consistent with the rest of the
 * codebase (mention.cluster_id, pos_annotation.token_id).
 */
@Entity
@Table(name = "wsd_annotation", indexes = {
        @Index(name = "idx_wsd_ann_token", columnList = "token_id"),
        @Index(name = "idx_wsd_ann_token_annotator", columnList = "token_id, annotator_id", unique = true),
        @Index(name = "idx_wsd_ann_workspace", columnList = "workspace_id"),
        @Index(name = "idx_wsd_ann_sense", columnList = "sense_id")
})
public class WsdAnnotationEntity extends BaseEntity {

    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "sense_id", nullable = false)
    private UUID senseId;

    @Column(name = "annotator_id", nullable = false, length = 100)
    private String annotatorId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @PrePersist
    @PreUpdate
    void touchTimestamp() {
        this.timestamp = Instant.now();
    }

    @Override
    public UUID getId() {
        return super.getId();
    }

    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }

    public UUID getSenseId() { return senseId; }
    public void setSenseId(UUID senseId) { this.senseId = senseId; }

    public String getAnnotatorId() { return annotatorId; }
    public void setAnnotatorId(String annotatorId) { this.annotatorId = annotatorId; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
