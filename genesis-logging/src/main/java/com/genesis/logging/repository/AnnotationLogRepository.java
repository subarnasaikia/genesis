package com.genesis.logging.repository;

import com.genesis.common.event.ActionType;
import com.genesis.logging.entity.AnnotationLogEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationLogRepository extends JpaRepository<AnnotationLogEntity, UUID> {

    /**
     * Paginated audit lookup for a workspace with optional filters.
     * Nulls disable individual filters. Ordered by timestamp DESC.
     */
    @Query("SELECT l FROM AnnotationLogEntity l "
            + "WHERE l.workspaceId = :workspaceId "
            + "AND (:actionType IS NULL OR l.actionType = :actionType) "
            + "AND (:fromInstant IS NULL OR l.timestamp >= :fromInstant) "
            + "AND (:toInstant IS NULL OR l.timestamp <= :toInstant) "
            + "ORDER BY l.timestamp DESC")
    Page<AnnotationLogEntity> findFiltered(
            @Param("workspaceId") UUID workspaceId,
            @Param("actionType") ActionType actionType,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant,
            Pageable pageable);
}
