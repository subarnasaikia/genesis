package com.genesis.workspace.dto;

import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.WorkspaceStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for workspace data.
 */
public class WorkspaceResponse {

    private UUID id;
    private String name;
    private String description;
    private AnnotationType annotationType;
    private WorkspaceStatus status;
    private UUID ownerId;
    private String ownerUsername;
    private Instant createdAt;
    private Instant updatedAt;

    // Progress statistics
    private long documentCount;
    private long annotatedDocumentCount;
    private int progressPercentage;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
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

    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public long getAnnotatedDocumentCount() {
        return annotatedDocumentCount;
    }

    public void setAnnotatedDocumentCount(long annotatedDocumentCount) {
        this.annotatedDocumentCount = annotatedDocumentCount;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
