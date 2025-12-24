package com.genesis.editor.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for editor session data.
 */
public class EditorSessionResponse {

    private UUID id;
    private UUID workspaceId;
    private UUID userId;
    private Integer lastDocumentIndex;
    private Integer scrollPosition;
    private Instant lastAccessedAt;

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getLastDocumentIndex() {
        return lastDocumentIndex;
    }

    public void setLastDocumentIndex(Integer lastDocumentIndex) {
        this.lastDocumentIndex = lastDocumentIndex;
    }

    public Integer getScrollPosition() {
        return scrollPosition;
    }

    public void setScrollPosition(Integer scrollPosition) {
        this.scrollPosition = scrollPosition;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
}
