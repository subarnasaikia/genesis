package com.genesis.workspace.repository;

import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Document entity operations.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Find all documents in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of documents
     */
    List<Document> findByWorkspaceId(UUID workspaceId);

    /**
     * Find all documents in a workspace ordered by orderIndex.
     *
     * @param workspaceId the workspace ID
     * @return list of documents ordered by orderIndex ascending
     */
    List<Document> findByWorkspaceIdOrderByOrderIndexAsc(UUID workspaceId);

    /**
     * Find documents by workspace and status.
     *
     * @param workspaceId the workspace ID
     * @param status      the document status
     * @return list of matching documents
     */
    List<Document> findByWorkspaceIdAndStatus(UUID workspaceId, DocumentStatus status);

    /**
     * Count documents in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return document count
     */
    long countByWorkspaceId(UUID workspaceId);

    /**
     * Find the maximum orderIndex in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return max order index if any documents exist
     */
    @Query("SELECT MAX(d.orderIndex) FROM Document d WHERE d.workspace.id = :workspaceId")
    Optional<Integer> findMaxOrderIndexByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    /**
     * Count documents in a workspace by status.
     *
     * @param workspaceId the workspace ID
     * @param status      the document status
     * @return document count with matching status
     */
    long countByWorkspaceIdAndStatus(UUID workspaceId, DocumentStatus status);
}
