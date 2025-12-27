package com.genesis.workspace.dto;

import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.entity.ProcessingStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for document data.
 */
public class DocumentResponse {

    private UUID id;
    private String name;
    private int orderIndex;
    private DocumentStatus status;
    private UUID workspaceId;
    private String storedFileUrl;
    private Integer tokenStartIndex;
    private Integer tokenEndIndex;
    private ProcessingStatus processingStatus;
    private String processingError;
    private Instant createdAt;
    private Instant updatedAt;

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

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getStoredFileUrl() {
        return storedFileUrl;
    }

    public void setStoredFileUrl(String storedFileUrl) {
        this.storedFileUrl = storedFileUrl;
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

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }
}
