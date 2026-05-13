package com.genesis.recommend.dto;

import java.util.UUID;

/**
 * Recommendation card surfaced to annotators.
 *
 * <p>{@code hash} is the SHA-256 spec hash (see {@code RecommendationHash}).
 * It's the dismissal key — stable across re-imports.
 */
public class RecommendationDto {

    private String hash;
    private RecommendationType type;
    private RecommendationPriority priority;
    private UUID documentId;
    private UUID entityId;
    private Integer tokenStartIndex;
    private Integer tokenEndIndex;
    private String reason;

    public RecommendationDto() {}

    public RecommendationDto(String hash,
            RecommendationType type,
            RecommendationPriority priority,
            UUID documentId,
            UUID entityId,
            Integer tokenStartIndex,
            Integer tokenEndIndex,
            String reason) {
        this.hash = hash;
        this.type = type;
        this.priority = priority;
        this.documentId = documentId;
        this.entityId = entityId;
        this.tokenStartIndex = tokenStartIndex;
        this.tokenEndIndex = tokenEndIndex;
        this.reason = reason;
    }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public RecommendationType getType() { return type; }
    public void setType(RecommendationType type) { this.type = type; }

    public RecommendationPriority getPriority() { return priority; }
    public void setPriority(RecommendationPriority priority) { this.priority = priority; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public Integer getTokenStartIndex() { return tokenStartIndex; }
    public void setTokenStartIndex(Integer tokenStartIndex) { this.tokenStartIndex = tokenStartIndex; }

    public Integer getTokenEndIndex() { return tokenEndIndex; }
    public void setTokenEndIndex(Integer tokenEndIndex) { this.tokenEndIndex = tokenEndIndex; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
