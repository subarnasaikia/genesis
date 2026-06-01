package com.genesis.recommend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-domain projection: token form counts across documents in a workspace.
 *
 * <p>This is a read-only projection whose only query joins
 * {@code TokenEntity} (genesis-import-export) with {@code Document}
 * (genesis-workspace) via JPQL — both are scanned by GenesisApplication's
 * {@code @EntityScan}, so JPQL can reference them by fully-qualified name. It
 * is backed directly by an {@link EntityManager} rather than extending
 * {@code JpaRepository}, because recommend owns no entity these queries
 * operate on. See ARCHITECTURE_AUDIT.md A-014.
 */
@Repository
public class TokenFormProjectionRepository {

    /** Query timeout in milliseconds — Rule 3 scans the full tokens table. */
    private static final int QUERY_TIMEOUT_MS = 2000;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns {@code [form, count]} rows for tokens whose surface form
     * appears at least {@code minCount} times in the workspace.
     *
     * <p>The 2-second query timeout is intentional — Rule 3 scans the full
     * {@code tokens} table, so we bound its runtime and let the orchestrator
     * skip the rule (via {@code QueryTimeoutException}) if it times out.
     */
    @Transactional(readOnly = true)
    public List<Object[]> findRepeatedTokenForms(UUID workspaceId, long minCount) {
        TypedQuery<Object[]> query = entityManager.createQuery(
                        "SELECT t.form, COUNT(t.id) "
                                + "FROM com.genesis.importexport.entity.TokenEntity t, "
                                + "     com.genesis.workspace.entity.Document d "
                                + "WHERE t.documentId = d.id "
                                + "  AND d.workspace.id = :workspaceId "
                                + "  AND t.form IS NOT NULL "
                                + "GROUP BY t.form "
                                + "HAVING COUNT(t.id) >= :minCount "
                                + "ORDER BY COUNT(t.id) DESC",
                        Object[].class)
                .setParameter("workspaceId", workspaceId)
                .setParameter("minCount", minCount);
        query.setHint("jakarta.persistence.query.timeout", QUERY_TIMEOUT_MS);
        return query.getResultList();
    }
}
