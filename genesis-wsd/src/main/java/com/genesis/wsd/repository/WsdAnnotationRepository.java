package com.genesis.wsd.repository;

import com.genesis.wsd.entity.WsdAnnotationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WsdAnnotationRepository extends JpaRepository<WsdAnnotationEntity, UUID> {

    long countBySenseId(UUID senseId);

    Optional<WsdAnnotationEntity> findByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);

    List<WsdAnnotationEntity> findByTokenId(UUID tokenId);

    List<WsdAnnotationEntity> findByWorkspaceId(UUID workspaceId);

    /**
     * All annotations for a document, across annotators. Backed by the
     * (workspace_id, document_id) composite index on the entity; no JOIN
     * across modules.
     */
    List<WsdAnnotationEntity> findByWorkspaceIdAndDocumentId(UUID workspaceId, UUID documentId);

    void deleteByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);

    /**
     * Per-annotator export rows: [tokenId, senseLabel, annotatorId]. The
     * surface form ("word") column is filled in by the service via
     * {@link com.genesis.common.port.TokenQueryPort} — keeping that join
     * out of JPQL means this module no longer references
     * {@code com.genesis.importexport}'s entity package.
     */
    @Query("SELECT a.tokenId, s.senseLabel, a.annotatorId "
            + "FROM WsdAnnotationEntity a, "
            + "     com.genesis.wsd.entity.WsdSenseEntity s "
            + "WHERE a.workspaceId = :workspaceId "
            + "  AND s.id = a.senseId "
            + "ORDER BY a.tokenId ASC, a.annotatorId ASC")
    List<Object[]> findPerAnnotatorExportRows(@Param("workspaceId") UUID workspaceId);

    /**
     * Consensus export rows grouped by (tokenId, senseId). Columns:
     * [tokenId, senseLabel, voteCount, maxTimestamp]. Ordered so that
     * the first row per tokenId is the majority winner; ties broken by
     * most recent timestamp. The service enriches each row with the
     * token's surface form via {@link com.genesis.common.port.TokenQueryPort}.
     */
    @Query("SELECT a.tokenId, s.senseLabel, COUNT(a), MAX(a.timestamp) "
            + "FROM WsdAnnotationEntity a, "
            + "     com.genesis.wsd.entity.WsdSenseEntity s "
            + "WHERE a.workspaceId = :workspaceId "
            + "  AND s.id = a.senseId "
            + "GROUP BY a.tokenId, a.senseId, s.senseLabel "
            + "ORDER BY a.tokenId ASC, COUNT(a) DESC, MAX(a.timestamp) DESC")
    List<Object[]> findConsensusExportRows(@Param("workspaceId") UUID workspaceId);
}
