package com.genesis.coref.dto;

import com.genesis.coref.entity.Cluster;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Cluster entity.
 */
public class ClusterResponse {

    private UUID id;
    private UUID workspaceId;
    private Integer clusterIndex;
    private Instant createdAt;
    private Instant updatedAt;

    // Default constructor
    public ClusterResponse() {
    }

    // Static factory method
    public static ClusterResponse fromEntity(Cluster cluster) {
        ClusterResponse response = new ClusterResponse();
        response.setId(cluster.getId());
        response.setWorkspaceId(cluster.getWorkspace().getId());
        response.setClusterIndex(cluster.getClusterIndex());
        response.setCreatedAt(cluster.getCreatedAt());
        response.setUpdatedAt(cluster.getUpdatedAt());
        return response;
    }

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

    public Integer getClusterIndex() {
        return clusterIndex;
    }

    public void setClusterIndex(Integer clusterIndex) {
        this.clusterIndex = clusterIndex;
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
