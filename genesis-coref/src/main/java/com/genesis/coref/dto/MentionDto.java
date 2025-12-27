package com.genesis.coref.dto;

import java.util.UUID;

/**
 * DTO for mention data.
 */
public class MentionDto {

    private UUID id;
    private UUID workspaceId;
    private UUID documentId;
    private UUID clusterId;
    private Integer clusterNumber; // From cluster for display
    private Integer sentenceIndex;
    private Integer startTokenIndex;
    private Integer endTokenIndex;
    private Integer globalStartIndex;
    private Integer globalEndIndex;
    private String text;
    private String mentionType;
    private String clusterColor; // From cluster for display

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getClusterId() {
        return clusterId;
    }

    public void setClusterId(UUID clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getClusterNumber() {
        return clusterNumber;
    }

    public void setClusterNumber(Integer clusterNumber) {
        this.clusterNumber = clusterNumber;
    }

    public Integer getSentenceIndex() {
        return sentenceIndex;
    }

    public void setSentenceIndex(Integer sentenceIndex) {
        this.sentenceIndex = sentenceIndex;
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

    public Integer getGlobalStartIndex() {
        return globalStartIndex;
    }

    public void setGlobalStartIndex(Integer globalStartIndex) {
        this.globalStartIndex = globalStartIndex;
    }

    public Integer getGlobalEndIndex() {
        return globalEndIndex;
    }

    public void setGlobalEndIndex(Integer globalEndIndex) {
        this.globalEndIndex = globalEndIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMentionType() {
        return mentionType;
    }

    public void setMentionType(String mentionType) {
        this.mentionType = mentionType;
    }

    public String getClusterColor() {
        return clusterColor;
    }

    public void setClusterColor(String clusterColor) {
        this.clusterColor = clusterColor;
    }
}
