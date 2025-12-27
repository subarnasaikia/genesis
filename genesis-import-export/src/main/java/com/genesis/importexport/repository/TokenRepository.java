package com.genesis.importexport.repository;

import com.genesis.importexport.entity.TokenEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for TokenEntity operations.
 */
@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, UUID> {

    /**
     * Find all tokens for a document ordered by global index.
     */
    List<TokenEntity> findByDocumentIdOrderByGlobalIndexAsc(UUID documentId);

    /**
     * Find all tokens for a document and sentence ordered by token index.
     */
    List<TokenEntity> findByDocumentIdAndSentenceIndexOrderByTokenIndexAsc(
            UUID documentId, Integer sentenceIndex);

    /**
     * Find tokens in a global index range (for span selection).
     */
    @Query("SELECT t FROM TokenEntity t WHERE t.documentId = :documentId " +
            "AND t.globalIndex >= :startIndex AND t.globalIndex <= :endIndex " +
            "ORDER BY t.globalIndex ASC")
    List<TokenEntity> findByDocumentIdAndGlobalIndexRange(
            @Param("documentId") UUID documentId,
            @Param("startIndex") Integer startIndex,
            @Param("endIndex") Integer endIndex);

    /**
     * Count tokens in a document.
     */
    long countByDocumentId(UUID documentId);

    /**
     * Get the maximum sentence index for a document.
     */
    @Query("SELECT MAX(t.sentenceIndex) FROM TokenEntity t WHERE t.documentId = :documentId")
    Integer findMaxSentenceIndexByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Get the maximum global index for a document.
     */
    @Query("SELECT MAX(t.globalIndex) FROM TokenEntity t WHERE t.documentId = :documentId")
    Integer findMaxGlobalIndexByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Delete all tokens for a document.
     */
    @Modifying
    @Query("DELETE FROM TokenEntity t WHERE t.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Check if document has tokens.
     */
    boolean existsByDocumentId(UUID documentId);
}
