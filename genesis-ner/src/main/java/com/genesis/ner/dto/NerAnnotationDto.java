package com.genesis.ner.dto;

import com.genesis.ner.entity.NerAnnotationEntity;
import java.time.Instant;
import java.util.UUID;

public class NerAnnotationDto {

    private UUID id;
    private UUID documentId;
    private Integer startTokenIndex;
    private Integer endTokenIndex;
    private String label;
    private String annotatorId;
    private Instant timestamp;

    public NerAnnotationDto() {
    }

    public NerAnnotationDto(UUID id, UUID documentId, Integer startTokenIndex,
            Integer endTokenIndex, String label, String annotatorId, Instant timestamp) {
        this.id = id;
        this.documentId = documentId;
        this.startTokenIndex = startTokenIndex;
        this.endTokenIndex = endTokenIndex;
        this.label = label;
        this.annotatorId = annotatorId;
        this.timestamp = timestamp;
    }

    public static NerAnnotationDto from(NerAnnotationEntity e) {
        if (e == null) {
            return null;
        }
        return new NerAnnotationDto(e.getId(), e.getDocumentId(), e.getStartTokenIndex(),
                e.getEndTokenIndex(), e.getLabel(), e.getAnnotatorId(), e.getTimestamp());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
