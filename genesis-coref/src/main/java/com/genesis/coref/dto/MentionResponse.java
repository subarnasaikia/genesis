package com.genesis.coref.dto;

import com.genesis.coref.entity.Mention;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Mention entity.
 */
public class MentionResponse {

    private UUID id;
    private UUID clusterId;
    private Integer tokenStartIndex;
    private Integer tokenEndIndex;
    private String text;
    private Instant createdAt;
    private Instant updatedAt;

    // Default constructor
    public MentionResponse() {
    }

    // Static factory method
    public static MentionResponse fromEntity(Mention mention) {
        MentionResponse response = new MentionResponse();
        response.setId(mention.getId());
        response.setClusterId(mention.getCluster().getId());
        response.setTokenStartIndex(mention.getTokenStartIndex());
        response.setTokenEndIndex(mention.getTokenEndIndex());
        response.setText(mention.getText());
        response.setCreatedAt(mention.getCreatedAt());
        response.setUpdatedAt(mention.getUpdatedAt());
        return response;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClusterId() {
        return clusterId;
    }

    public void setClusterId(UUID clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getTokenStartIndex() {
        return tokenStartIndex;
    }

    public void setTokenStartIndex(Integer tokenStartIndex) {
        this.tokenStartIndex = tokenStartIndex;
    }

    public Integer getTokenEndIndex() {
        return tokenEndIndex;
    }

    public void setTokenEndIndex(Integer tokenEndIndex) {
        this.tokenEndIndex = tokenEndIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
