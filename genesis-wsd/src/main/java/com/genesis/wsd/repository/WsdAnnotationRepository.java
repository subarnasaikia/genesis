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

    void deleteByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);

    /**
     * Per-annotator export rows: [tokenId, word, senseLabel, annotatorId].
     * Joins WsdAnnotation with TokenEntity and WsdSenseEntity.
     */
    @Query("SELECT a.tokenId, t.form, s.senseLabel, a.annotatorId "
            + "FROM WsdAnnotationEntity a, "
            + "     com.genesis.importexport.entity.TokenEntity t, "
            + "     com.genesis.wsd.entity.WsdSenseEntity s "
            + "WHERE a.workspaceId = :workspaceId "
            + "  AND t.id = a.tokenId "
            + "  AND s.id = a.senseId "
            + "ORDER BY a.tokenId ASC, a.annotatorId ASC")
    List<Object[]> findPerAnnotatorExportRows(@Param("workspaceId") UUID workspaceId);

    /**
     * Consensus export rows grouped by (tokenId, senseId). Columns:
     * [tokenId, word, senseLabel, voteCount, maxTimestamp]. Ordered so
     * that the first row per tokenId is the majority winner; ties
     * broken by most recent timestamp.
     */
    @Query("SELECT a.tokenId, t.form, s.senseLabel, COUNT(a), MAX(a.timestamp) "
            + "FROM WsdAnnotationEntity a, "
            + "     com.genesis.importexport.entity.TokenEntity t, "
            + "     com.genesis.wsd.entity.WsdSenseEntity s "
            + "WHERE a.workspaceId = :workspaceId "
            + "  AND t.id = a.tokenId "
            + "  AND s.id = a.senseId "
            + "GROUP BY a.tokenId, t.form, a.senseId, s.senseLabel "
            + "ORDER BY a.tokenId ASC, COUNT(a) DESC, MAX(a.timestamp) DESC")
    List<Object[]> findConsensusExportRows(@Param("workspaceId") UUID workspaceId);
}
