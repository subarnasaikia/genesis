package com.genesis.importexport.repository;

import com.genesis.importexport.entity.SentenceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for SentenceEntity operations.
 */
@Repository
public interface SentenceRepository extends JpaRepository<SentenceEntity, UUID> {

    /**
     * Find all sentences for a document ordered by sentence index.
     */
    List<SentenceEntity> findByDocumentIdOrderBySentenceIndexAsc(UUID documentId);

    /**
     * Find a specific sentence by document and index.
     */
    Optional<SentenceEntity> findByDocumentIdAndSentenceIndex(UUID documentId, Integer sentenceIndex);

    /**
     * Count sentences in a document.
     */
    long countByDocumentId(UUID documentId);

    /**
     * Get the maximum sentence index for a document.
     */
    @Query("SELECT MAX(s.sentenceIndex) FROM SentenceEntity s WHERE s.documentId = :documentId")
    Integer findMaxSentenceIndexByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Delete all sentences for a document.
     */
    @Modifying
    @Query("DELETE FROM SentenceEntity s WHERE s.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Check if document has sentences.
     */
    boolean existsByDocumentId(UUID documentId);
}
