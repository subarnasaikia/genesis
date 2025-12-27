package com.genesis.coref.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Represents a coreference cluster (entity chain).
 *
 * <p>
 * A cluster groups mentions that refer to the same real-world entity.
 * Clusters are workspace-scoped and can span across multiple documents.
 */
@Entity
@Table(name = "coref_clusters", indexes = {
        @Index(name = "idx_cluster_workspace", columnList = "workspace_id"),
        @Index(name = "idx_cluster_workspace_number", columnList = "workspace_id, cluster_number", unique = true)
})
public class ClusterEntity extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    /**
     * Cluster number for CoNLL export (1, 2, 3...).
     * Unique within a workspace.
     */
    @Column(name = "cluster_number", nullable = false)
    private Integer clusterNumber;

    /**
     * Optional label/name for the cluster (e.g., "Barack Obama", "The President").
     */
    @Column(name = "label", length = 500)
    private String label;

    /**
     * Optional representative text (typically the longest/most descriptive
     * mention).
     */
    @Column(name = "representative_text", length = 1000)
    private String representativeText;

    /**
     * UI display color (hex format, e.g., "#FF5733").
     */
    @Column(name = "color", length = 20)
    private String color;

    /**
     * Number of mentions in this cluster (cached for performance).
     */
    @Column(name = "mention_count")
    private Integer mentionCount;

    // Getters and Setters

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
