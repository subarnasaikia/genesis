package com.genesis.coref.dto;

import java.util.UUID;

/**
 * DTO for cluster data.
 */
public class ClusterDto {

    private UUID id;
    private UUID workspaceId;
    private Integer clusterNumber;
    private String label;
    private String representativeText;
    private String color;
    private Integer mentionCount;

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

    public Integer getClusterNumber() {
        return clusterNumber;
    }

    public void setClusterNumber(Integer clusterNumber) {
        this.clusterNumber = clusterNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRepresentativeText() {
        return representativeText;
    }

    public void setRepresentativeText(String representativeText) {
        this.representativeText = representativeText;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getMentionCount() {
        return mentionCount;
    }

    public void setMentionCount(Integer mentionCount) {
        this.mentionCount = mentionCount;
    }
}
