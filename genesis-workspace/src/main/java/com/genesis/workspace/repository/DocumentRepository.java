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

    /**
     * Per-workspace document totals: total count and count in a given status,
     * for many workspaces in a single query (C-008). Mapping a list of workspaces
     * with the two single-workspace count methods above fires 2 queries per
     * workspace (a classic N+1); this folds all of them into one grouped query.
     *
     * <p>Workspaces with no documents produce no row — callers treat a missing
     * entry as zero counts.
     */
    interface WorkspaceDocumentCounts {
        UUID getWorkspaceId();

        long getTotal();

        long getCompleted();
    }

    /**
     * Batch the per-workspace document counts for the given workspaces.
     *
     * @param workspaceIds    the workspaces to count documents for
     * @param completedStatus the status counted as "completed"
     * @return one row per workspace that has at least one document
     */
    @Query("SELECT d.workspace.id AS workspaceId, COUNT(d) AS total, "
            + "SUM(CASE WHEN d.status = :completedStatus THEN 1 ELSE 0 END) AS completed "
            + "FROM Document d WHERE d.workspace.id IN :workspaceIds GROUP BY d.workspace.id")
    List<WorkspaceDocumentCounts> countsByWorkspaceIds(
            @Param("workspaceIds") List<UUID> workspaceIds,
            @Param("completedStatus") DocumentStatus completedStatus);
}
