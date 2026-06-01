package com.genesis.recommend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-domain projection for Rule 4 (COREF_CHAIN_GAP).
 *
 * <p>This is a read-only projection that joins clusters, mentions, and
 * documents owned by other modules. It is backed directly by an
 * {@link EntityManager} rather than extending {@code JpaRepository}: there is
 * no recommend-owned entity these queries operate on, so anchoring them to an
 * unrelated entity root would be a lie (and would expose CRUD methods that
 * could mutate that unrelated table). See ARCHITECTURE_AUDIT.md A-014.
 */
@Repository
public class ClusterChainProjectionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns {@code [clusterId, minOrderIndex, maxOrderIndex,
     * distinctDocCount]} for clusters whose mentions skip at least one
     * document in the workspace's ordered sequence — i.e.
     * {@code max - min + 1 > distinctDocs}.
     */
    @Transactional(readOnly = true)
    public List<Object[]> findClustersWithChainGaps(UUID workspaceId) {
        return entityManager.createQuery(
                        "SELECT c.id, MIN(d.orderIndex), MAX(d.orderIndex), COUNT(DISTINCT d.id) "
                                + "FROM com.genesis.coref.entity.ClusterEntity c, "
                                + "     com.genesis.coref.entity.MentionEntity m, "
                                + "     com.genesis.workspace.entity.Document d "
                                + "WHERE m.clusterId = c.id "
                                + "  AND d.id = m.documentId "
                                + "  AND c.workspaceId = :workspaceId "
                                + "GROUP BY c.id "
                                + "HAVING (MAX(d.orderIndex) - MIN(d.orderIndex) + 1) > COUNT(DISTINCT d.id)",
                        Object[].class)
                .setParameter("workspaceId", workspaceId)
                .getResultList();
    }
}
