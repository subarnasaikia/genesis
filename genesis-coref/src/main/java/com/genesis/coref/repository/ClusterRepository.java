package com.genesis.coref.repository;

import com.genesis.coref.entity.ClusterEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ClusterEntity operations.
 */
@Repository
public interface ClusterRepository extends JpaRepository<ClusterEntity, UUID> {

    /**
     * Find all clusters for a workspace ordered by cluster number.
     */
    List<ClusterEntity> findByWorkspaceIdOrderByClusterNumberAsc(UUID workspaceId);

    /**
     * Keyset page of clusters for a workspace, ordered by cluster number (unique
     * per workspace, so a stable keyset). Pass the previous page's last cluster
     * number as {@code cursor} (or {@code null} for the first page); size the
     * {@link Pageable} to {@code limit + 1} to detect whether more pages remain.
     */
    @Query("SELECT c FROM ClusterEntity c WHERE c.workspaceId = :workspaceId "
            + "AND (:cursor IS NULL OR c.clusterNumber > :cursor) ORDER BY c.clusterNumber ASC")
    List<ClusterEntity> findPageByWorkspaceId(
            @Param("workspaceId") UUID workspaceId,
            @Param("cursor") Integer cursor,
            Pageable pageable);

    /**
     * Find a cluster by workspace and cluster number.
     */
    Optional<ClusterEntity> findByWorkspaceIdAndClusterNumber(UUID workspaceId, Integer clusterNumber);

    /**
     * Get the next available cluster number for a workspace.
     */
    @Query("SELECT COALESCE(MAX(c.clusterNumber), 0) + 1 FROM ClusterEntity c WHERE c.workspaceId = :workspaceId")
    Integer getNextClusterNumber(@Param("workspaceId") UUID workspaceId);

    /**
     * Count clusters in a workspace.
     */
    long countByWorkspaceId(UUID workspaceId);

    /**
     * Delete all clusters for a workspace.
     */
    void deleteByWorkspaceId(UUID workspaceId);

    /**
     * Check if a cluster exists.
     */
    boolean existsByWorkspaceIdAndClusterNumber(UUID workspaceId, Integer clusterNumber);
}
