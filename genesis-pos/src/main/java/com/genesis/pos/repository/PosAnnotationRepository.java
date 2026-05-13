package com.genesis.pos.repository;

import com.genesis.pos.entity.PosAnnotationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PosAnnotationRepository extends JpaRepository<PosAnnotationEntity, UUID> {

    Optional<PosAnnotationEntity> findByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);

    List<PosAnnotationEntity> findByTokenId(UUID tokenId);

    List<PosAnnotationEntity> findByDocumentId(UUID documentId);

    void deleteByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);

    /**
     * Returns rows of [tokenId, posTag, count, mostRecentTimestamp] grouped by
     * (tokenId, posTag). Ordered such that the first row per tokenId is the
     * majority winner; ties broken by most recent timestamp.
     */
    @Query("SELECT p.tokenId, p.posTag, COUNT(p), MAX(p.timestamp) "
            + "FROM PosAnnotationEntity p "
            + "WHERE p.documentId = :documentId "
            + "GROUP BY p.tokenId, p.posTag "
            + "ORDER BY p.tokenId ASC, COUNT(p) DESC, MAX(p.timestamp) DESC")
    List<Object[]> findPosCountsByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Returns rows of [tokenId, distinctAnnotatorCount] for the document.
     * Powers the sidecar confidence CSV: consumers can filter out tokens
     * tagged by only a single annotator (no consensus).
     */
    @Query("SELECT p.tokenId, COUNT(DISTINCT p.annotatorId) "
            + "FROM PosAnnotationEntity p "
            + "WHERE p.documentId = :documentId "
            + "GROUP BY p.tokenId")
    List<Object[]> findAnnotatorCountsByDocumentId(@Param("documentId") UUID documentId);
}
