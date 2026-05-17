package com.genesis.ner.entity;

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
 * A single NER span annotation by one annotator: tokens
 * {@code [startTokenIndex .. endTokenIndex]} (inclusive, global indices within
 * the document) carry {@code label}.
 *
 * <p>Spans may nest and overlap freely: there is no uniqueness on
 * {@code (document_id, start_token_index, end_token_index, annotator_id)} or
 * across annotators. The same annotator can have multiple spans on the same
 * tokens with different labels — this is intentional to support hierarchical
 * entities (e.g. "[Microsoft CEO Satya Nadella]" containing
 * "[Microsoft CEO]" and "[Satya Nadella]").
 *
 * <p>{@code end_token_index >= start_token_index} is enforced by a Postgres
 * CHECK constraint in the V3 migration; this entity does not duplicate it
 * because Hibernate runs in {@code validate} mode and would not regenerate the
 * constraint if it changed.
 */
@Entity
@Table(name = "ner_annotations", indexes = {
        @Index(name = "idx_ner_ann_document", columnList = "document_id"),
        @Index(name = "idx_ner_ann_doc_annotator", columnList = "document_id, annotator_id")
})
public class NerAnnotationEntity extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "start_token_index", nullable = false)
    private Integer startTokenIndex;

    @Column(name = "end_token_index", nullable = false)
    private Integer endTokenIndex;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Column(name = "annotator_id", nullable = false, length = 100)
    private String annotatorId;

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

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public Integer getStartTokenIndex() {
        return startTokenIndex;
    }

    public void setStartTokenIndex(Integer startTokenIndex) {
        this.startTokenIndex = startTokenIndex;
    }

    public Integer getEndTokenIndex() {
        return endTokenIndex;
    }

    public void setEndTokenIndex(Integer endTokenIndex) {
        this.endTokenIndex = endTokenIndex;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(String annotatorId) {
        this.annotatorId = annotatorId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
