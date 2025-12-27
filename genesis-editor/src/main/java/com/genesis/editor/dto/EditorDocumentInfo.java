package com.genesis.editor.dto;

import java.util.UUID;

/**
 * Document information for editor display.
 */
public class EditorDocumentInfo {

    private UUID id;
    private String name;
    private Integer orderIndex;
    private String status;
    private Integer sentenceCount;
    private Integer tokenCount;
    private Boolean isTokenized;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSentenceCount() {
        return sentenceCount;
    }

    public void setSentenceCount(Integer sentenceCount) {
        this.sentenceCount = sentenceCount;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Boolean getIsTokenized() {
        return isTokenized;
    }

    public void setIsTokenized(Boolean isTokenized) {
        this.isTokenized = isTokenized;
    }
}
