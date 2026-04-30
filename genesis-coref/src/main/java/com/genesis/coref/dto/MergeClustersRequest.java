package com.genesis.coref.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to merge one or more source clusters into a target cluster.
 *
 * <p>
 * All mentions belonging to the source clusters are reassigned to the target,
 * the source clusters are deleted, and the remaining clusters in the workspace
 * are renumbered contiguously.
 */
public class MergeClustersRequest {

    private List<UUID> sourceClusterIds;
    private UUID targetClusterId;

    public List<UUID> getSourceClusterIds() {
        return sourceClusterIds;
    }

    public void setSourceClusterIds(List<UUID> sourceClusterIds) {
        this.sourceClusterIds = sourceClusterIds;
    }

    public UUID getTargetClusterId() {
        return targetClusterId;
    }

    public void setTargetClusterId(UUID targetClusterId) {
        this.targetClusterId = targetClusterId;
    }
}
