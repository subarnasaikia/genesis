package com.genesis.coref.repository;

import com.genesis.coref.entity.MentionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for MentionEntity operations.
 */
@Repository
public interface MentionRepository extends JpaRepository<MentionEntity, UUID> {

        /**
         * Find all mentions for a workspace.
         */
        List<MentionEntity> findByWorkspaceId(UUID workspaceId);

        /**
         * Keyset page of mentions for a workspace, ordered by primary key. Pass
         * the previous page's last id as {@code cursor} (or {@code null} for the
         * first page); size the {@link Pageable} to {@code limit + 1} so the
         * caller can detect whether another page exists.
         */
        @Query("SELECT m FROM MentionEntity m WHERE m.workspaceId = :workspaceId "
                        + "AND (:cursor IS NULL OR m.id > :cursor) ORDER BY m.id ASC")
        List<MentionEntity> findPageByWorkspaceId(
                        @Param("workspaceId") UUID workspaceId,
                        @Param("cursor") UUID cursor,
                        Pageable pageable);

        /**
         * Find all mentions for a document ordered by sentence and token index.
         */
        @Query("SELECT m FROM MentionEntity m WHERE m.documentId = :documentId " +
                        "ORDER BY m.sentenceIndex ASC, m.startTokenIndex ASC")
        List<MentionEntity> findByDocumentIdOrdered(@Param("documentId") UUID documentId);

        /**
         * Find all mentions in a cluster.
         */
        List<MentionEntity> findByClusterId(UUID clusterId);

        /**
         * Find all mentions in a cluster ordered by document and position.
         */
        @Query("SELECT m FROM MentionEntity m WHERE m.clusterId = :clusterId " +
                        "ORDER BY m.documentId ASC, m.sentenceIndex ASC, m.startTokenIndex ASC")
        List<MentionEntity> findByClusterIdOrdered(@Param("clusterId") UUID clusterId);

        /**
         * Find mentions in a specific sentence.
         */
        List<MentionEntity> findByDocumentIdAndSentenceIndex(UUID documentId, Integer sentenceIndex);

        /**
         * Find unassigned mentions (no cluster).
         */
        List<MentionEntity> findByWorkspaceIdAndClusterIdIsNull(UUID workspaceId);

        /**
         * Count mentions in a cluster.
         */
        long countByClusterId(UUID clusterId);

        /**
         * Count mentions in a workspace.
         */
        long countByWorkspaceId(UUID workspaceId);

        /**
         * Count mentions in a document.
         */
        long countByDocumentId(UUID documentId);

        /**
         * Delete all mentions in a cluster.
         */
        @Modifying
        void deleteByClusterId(UUID clusterId);

        /**
         * Delete all mentions for a workspace.
         */
        @Modifying
        void deleteByWorkspaceId(UUID workspaceId);

        /**
         * Delete all mentions for a document.
         */
        @Modifying
        void deleteByDocumentId(UUID documentId);

        /**
         * Unassign mentions from a cluster (set clusterId to null).
         */
        @Modifying
        @Query("UPDATE MentionEntity m SET m.clusterId = null WHERE m.clusterId = :clusterId")
        void unassignFromCluster(@Param("clusterId") UUID clusterId);

        /**
         * Reassign every mention currently in any of the source clusters to the
         * target cluster in a single batch UPDATE.
         *
         * @param targetId  the destination cluster id
         * @param sourceIds the source cluster ids whose mentions are to be moved
         * @return number of mention rows updated
         */
        @Modifying
        @Query("UPDATE MentionEntity m SET m.clusterId = :targetId WHERE m.clusterId IN :sourceIds")
        int reassignMentionsToCluster(@Param("targetId") UUID targetId, @Param("sourceIds") List<UUID> sourceIds);

        /**
         * Check if a mention overlaps with existing mentions in the same sentence.
         */
        @Query("SELECT COUNT(m) > 0 FROM MentionEntity m WHERE m.documentId = :documentId " +
                        "AND m.sentenceIndex = :sentenceIndex " +
                        "AND ((m.startTokenIndex <= :endToken AND m.endTokenIndex >= :startToken))")
        boolean hasOverlappingMention(
                        @Param("documentId") UUID documentId,
                        @Param("sentenceIndex") Integer sentenceIndex,
                        @Param("startToken") Integer startToken,
                        @Param("endToken") Integer endToken);

        /**
         * Calculate sum of token lengths for all mentions in a document.
         */
        @Query("SELECT COALESCE(SUM(m.endTokenIndex - m.startTokenIndex + 1), 0) FROM MentionEntity m WHERE m.documentId = :documentId")
        Long sumMentionTokensByDocumentId(@Param("documentId") UUID documentId);
}
