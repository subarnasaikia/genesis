package com.genesis.coref.repository;

import com.genesis.coref.entity.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Cluster entity operations.
 */
@Repository
public interface ClusterRepository extends JpaRepository<Cluster, UUID> {

    /**
     * Find all clusters for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of clusters
     */
    List<Cluster> findByWorkspaceId(UUID workspaceId);

    /**
     * Delete all clusters for a workspace.
     *
     * @param workspaceId the workspace ID
     */
    void deleteByWorkspaceId(UUID workspaceId);

    /**
     * Count clusters for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return cluster count
     */
    long countByWorkspaceId(UUID workspaceId);
}
