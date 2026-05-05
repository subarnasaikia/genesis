package com.genesis.pos.entity;

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
 * Per-annotator part-of-speech tag for a token.
 *
 * <p>One row per (token, annotator). Multiple annotators may tag the same token
 * with different POS tags; majority vote is computed at export time.
 */
@Entity
@Table(name = "pos_annotations", indexes = {
        @Index(name = "idx_pos_token", columnList = "token_id"),
        @Index(name = "idx_pos_token_annotator", columnList = "token_id, annotator_id", unique = true),
        @Index(name = "idx_pos_document", columnList = "document_id")
})
public class PosAnnotationEntity extends BaseEntity {

    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "annotator_id", nullable = false, length = 100)
    private String annotatorId;

    @Column(name = "pos_tag", nullable = false, length = 20)
    private String posTag;

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

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(String annotatorId) {
        this.annotatorId = annotatorId;
    }

    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
