package com.genesis.editor.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to save editor session state.
 */
public class SaveSessionRequest {

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    private Integer lastDocumentIndex;

    private Integer scrollPosition;

    // Getters and Setters

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
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
}
