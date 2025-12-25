package com.genesis.coref.entity;

import com.genesis.common.entity.BaseEntity;
import com.genesis.workspace.entity.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cluster entity representing a coreference cluster (chain).
 *
 * <p>
 * A cluster groups multiple mentions that refer to the same entity.
 * For example, "John", "he", and "the CEO" might all be in the same cluster
 * if they refer to the same person.
 */
@Entity
@Table(name = "clusters", indexes = {
        @Index(name = "idx_clusters_workspace_id", columnList = "workspace_id")
})
public class Cluster extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /**
     * Optional display index for this cluster (e.g., "Chain 1", "Chain 2").
     * Used for UI purposes to show cluster numbers.
     */
    @Column(name = "cluster_index")
    private Integer clusterIndex;

    // Default constructor required by JPA
    public Cluster() {
    }

    // Getters and Setters

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Integer getClusterIndex() {
        return clusterIndex;
    }

    public void setClusterIndex(Integer clusterIndex) {
        this.clusterIndex = clusterIndex;
    }
}
