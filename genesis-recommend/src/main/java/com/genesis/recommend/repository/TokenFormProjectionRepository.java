package com.genesis.recommend.repository;

import com.genesis.recommend.entity.DismissedRecommendationEntity;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Cross-domain projection: token form counts across documents in a workspace.
 *
 * <p>Reuses {@code DismissedRecommendationEntity} as the JpaRepository root
 * only because Spring Data requires an entity type; the only method here is
 * a custom @Query that joins {@code TokenEntity} (genesis-import-export)
 * with {@code Document} (genesis-workspace) via JPQL — both entities are
 * scanned by GenesisApplication's @EntityScan, so JPQL can reference them
 * without a Java import.
 */
@Repository
public interface TokenFormProjectionRepository
        extends JpaRepository<DismissedRecommendationEntity, UUID> {

    /**
     * Returns {@code [form, count]} rows for tokens whose surface form
     * appears at least {@code minCount} times in the workspace.
     *
     * <p>The 2-second query timeout is intentional — Rule 3 scans the
     * full {@code tokens} table, so we bound its runtime and let the
     * orchestrator skip the rule if it times out.
     */
    @Query("SELECT t.form, COUNT(t.id) "
            + "FROM com.genesis.importexport.entity.TokenEntity t, "
            + "     com.genesis.workspace.entity.Document d "
            + "WHERE t.documentId = d.id "
            + "  AND d.workspace.id = :workspaceId "
            + "  AND t.form IS NOT NULL "
            + "GROUP BY t.form "
            + "HAVING COUNT(t.id) >= :minCount "
            + "ORDER BY COUNT(t.id) DESC")
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.query.timeout", value = "2000")
    })
    List<Object[]> findRepeatedTokenForms(
            @Param("workspaceId") UUID workspaceId,
            @Param("minCount") long minCount);
}
