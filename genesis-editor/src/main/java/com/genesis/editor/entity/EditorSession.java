package com.genesis.editor.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track user's editor session state for a workspace.
 *
 * <p>This is a lightweight entity for UX purposes, tracking last position
 * within the workspace's documents. Annotation data is stored separately
 * in the coref module.
 */
@Entity
@Table(name = "editor_sessions", indexes = {
    @Index(name = "idx_editor_sessions_workspace_user",
           columnList = "workspace_id, user_id", unique = true),
    @Index(name = "idx_editor_sessions_last_accessed", columnList = "last_accessed_at")
})
public class EditorSession extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The last document index the user was viewing (0-based).
     */
    @Column(name = "last_document_index")
    private Integer lastDocumentIndex;

    /**
     * Scroll position within the document (token index).
     */
    @Column(name = "scroll_position")
    private Integer scrollPosition;

    /**
     * Last time the user accessed this workspace in the editor.
     */
    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    // Default constructor required by JPA
    public EditorSession() {
    }

    // Getters and Setters

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
