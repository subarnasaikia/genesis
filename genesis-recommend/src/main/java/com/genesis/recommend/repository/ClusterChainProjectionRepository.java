package com.genesis.recommend.repository;

import com.genesis.recommend.entity.DismissedRecommendationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Cross-domain projection for Rule 4 (COREF_CHAIN_GAP).
 *
 * <p>Reuses {@code DismissedRecommendationEntity} as the JpaRepository
 * root only to satisfy Spring Data; the actual query joins clusters,
 * mentions, and documents from other modules via JPQL.
 */
@Repository
public interface ClusterChainProjectionRepository
        extends JpaRepository<DismissedRecommendationEntity, UUID> {

    /**
     * Returns {@code [clusterId, minOrderIndex, maxOrderIndex,
     * distinctDocCount]} for clusters whose mentions skip at least one
     * document in the workspace's ordered sequence — i.e.
     * {@code max - min + 1 > distinctDocs}.
     */
    @Query("SELECT c.id, MIN(d.orderIndex), MAX(d.orderIndex), COUNT(DISTINCT d.id) "
            + "FROM com.genesis.coref.entity.ClusterEntity c, "
            + "     com.genesis.coref.entity.MentionEntity m, "
            + "     com.genesis.workspace.entity.Document d "
            + "WHERE m.clusterId = c.id "
            + "  AND d.id = m.documentId "
            + "  AND c.workspaceId = :workspaceId "
            + "GROUP BY c.id "
            + "HAVING (MAX(d.orderIndex) - MIN(d.orderIndex) + 1) > COUNT(DISTINCT d.id)")
    List<Object[]> findClustersWithChainGaps(@Param("workspaceId") UUID workspaceId);
}
