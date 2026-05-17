package com.genesis.ner.dto;

import java.util.UUID;

public class CreateNerAnnotationRequest {

    private UUID documentId;
    private Integer startTokenIndex;
    private Integer endTokenIndex;
    private String label;

    public CreateNerAnnotationRequest() {
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
}
