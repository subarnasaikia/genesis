package com.genesis.coref.service;

import com.genesis.coref.dto.ClusterDto;
import com.genesis.coref.dto.CreateClusterRequest;
import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.event.ActionType;
import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.common.event.WorkspaceActivityEvent;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import com.genesis.workspace.service.WorkspaceAccessControl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for cluster operations.
 */
@Service
public class ClusterService {

    private static final String[] DEFAULT_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    };

    private final ClusterRepository clusterRepository;
    private final MentionRepository mentionRepository;
    private final WorkspaceAccessControl accessControl;
    private final ApplicationEventPublisher eventPublisher;

    public ClusterService(ClusterRepository clusterRepository,
            MentionRepository mentionRepository,
            WorkspaceAccessControl accessControl,
            ApplicationEventPublisher eventPublisher) {
        this.clusterRepository = clusterRepository;
        this.mentionRepository = mentionRepository;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new cluster.
     */
    @Transactional
    public ClusterDto createCluster(@NonNull UUID workspaceId,
            CreateClusterRequest request,
            @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        Integer nextNumber = clusterRepository.getNextClusterNumber(workspaceId);

        ClusterEntity cluster = new ClusterEntity();
        cluster.setWorkspaceId(workspaceId);
        cluster.setClusterNumber(nextNumber);
        cluster.setLabel(request != null ? request.getLabel() : null);
        cluster.setColor(
                request != null && request.getColor() != null ? request.getColor() : getDefaultColor(nextNumber));
        cluster.setMentionCount(0);

        ClusterEntity saved = clusterRepository.save(cluster);
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));

        // Audit log: cluster created
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                workspaceId,
                currentUser(),
                ActionType.CLUSTER_CREATED,
                saved.getId(),
                String.format("{\"clusterNumber\":%d,\"label\":%s}",
                        saved.getClusterNumber(),
                        saved.getLabel() == null ? "null" : "\"" + escape(saved.getLabel()) + "\"")));
        return mapToDto(saved);
    }

    /**
     * Get all clusters for a workspace.
     */
    public List<ClusterDto> getClusters(@NonNull UUID workspaceId, @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        return clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get cluster by ID.
     */
    public ClusterDto getCluster(@NonNull UUID clusterId, @NonNull UUID callerId) {
        ClusterEntity cluster = findClusterById(clusterId);
        accessControl.requireMember(cluster.getWorkspaceId(), callerId);
        return mapToDto(cluster);
    }

    /**
     * Update cluster.
     */
    @Transactional
    public ClusterDto updateCluster(@NonNull UUID clusterId,
            CreateClusterRequest request,
            @NonNull UUID callerId) {
        ClusterEntity cluster = findClusterById(clusterId);
        accessControl.requireMember(cluster.getWorkspaceId(), callerId);

        if (request.getLabel() != null) {
            cluster.setLabel(request.getLabel());
        }
        if (request.getColor() != null) {
            cluster.setColor(request.getColor());
        }

        ClusterEntity saved = clusterRepository.save(cluster);
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, saved.getWorkspaceId()));
        return mapToDto(saved);
    }

    /**
     * Delete cluster. Mentions are unassigned (not deleted). Remaining clusters
     * are renumbered contiguously so the visible cluster numbers stay consistent
     * (1, 2, 3, ...) with no gaps.
     */
    @Transactional
    public void deleteCluster(@NonNull UUID clusterId, @NonNull UUID callerId) {
        ClusterEntity cluster = findClusterById(clusterId);
        accessControl.requireMember(cluster.getWorkspaceId(), callerId);

        // Unassign all mentions from this cluster
        mentionRepository.unassignFromCluster(clusterId);

        UUID workspaceId = cluster.getWorkspaceId();
        clusterRepository.delete(cluster);

        // Compact remaining cluster numbers so they stay contiguous (1, 2, 3, ...).
        compactClusterNumbers(workspaceId);

        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));
    }

    /**
     * Merge one or more source clusters into a target cluster.
     *
     * <p>
     * All mentions belonging to the source clusters are reassigned to the target
     * via a single batch UPDATE, the target's cached {@code mentionCount} is
     * increased by the sum of the source counts, the source clusters are deleted,
     * and the remaining clusters in the workspace are renumbered contiguously.
     *
     * @param workspaceId the workspace that owns all clusters in the merge
     * @param sourceIds   ids of the clusters to merge into the target (must be
     *                    non-empty and must not contain {@code targetId})
     * @param targetId    the surviving cluster's id
     * @return DTO for the surviving target cluster, with updated mention count
     * @throws ValidationException       if {@code sourceIds} is null/empty, contains
     *                                   {@code targetId}, or any cluster's
     *                                   workspace differs from {@code workspaceId}
     * @throws ResourceNotFoundException if any cluster id cannot be found
     */
    @Transactional
    public ClusterDto mergeClusters(
            @NonNull UUID workspaceId,
            List<UUID> sourceIds,
            UUID targetId,
            @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);

        if (targetId == null) {
            throw new ValidationException("targetClusterId must not be null");
        }
        if (sourceIds == null || sourceIds.isEmpty()) {
            throw new ValidationException("sourceClusterIds must not be empty");
        }

        // Deduplicate source ids and reject self-merge.
        Set<UUID> uniqueSourceIds = new HashSet<>(sourceIds);
        if (uniqueSourceIds.contains(targetId)) {
            throw new ValidationException(
                    "targetClusterId must not appear in sourceClusterIds (self-merge)");
        }
        List<UUID> dedupedSourceIds = new ArrayList<>(uniqueSourceIds);

        // Load + validate target.
        ClusterEntity target = findClusterById(targetId);
        if (!workspaceId.equals(target.getWorkspaceId())) {
            throw new ValidationException(
                    "targetClusterId does not belong to workspace " + workspaceId);
        }

        // Load + validate sources. Compute mentionCount delta BEFORE deletion.
        int targetMentionCountDelta = 0;
        List<ClusterEntity> sources = new ArrayList<>(dedupedSourceIds.size());
        for (UUID sourceId : dedupedSourceIds) {
            ClusterEntity source = findClusterById(sourceId);
            if (!workspaceId.equals(source.getWorkspaceId())) {
                throw new ValidationException(
                        "sourceClusterId " + sourceId + " does not belong to workspace " + workspaceId);
            }
            Integer count = source.getMentionCount();
            if (count != null) {
                targetMentionCountDelta += count;
            }
            sources.add(source);
        }

        // Single batch UPDATE — no N individual saves.
        mentionRepository.reassignMentionsToCluster(targetId, dedupedSourceIds);

        // Update the target's cached count.
        Integer existing = target.getMentionCount() != null ? target.getMentionCount() : 0;
        target.setMentionCount(existing + targetMentionCountDelta);
        ClusterEntity savedTarget = clusterRepository.save(target);

        // Delete source clusters. clusterId on MentionEntity is a plain UUID column
        // (no FK constraint), so this is safe after the batch reassignment above.
        clusterRepository.deleteAll(sources);

        // Renumber remaining clusters so numbers stay contiguous.
        compactClusterNumbers(workspaceId);

        // Re-load the target so the DTO reflects any cluster_number changes from
        // compaction.
        ClusterEntity refreshed = clusterRepository.findById(savedTarget.getId())
                .orElse(savedTarget);

        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));

        // Audit log: cluster merged
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                workspaceId,
                currentUser(),
                ActionType.CLUSTER_MERGED,
                refreshed.getId(),
                String.format("{\"sourceIds\":[%s],\"targetId\":\"%s\",\"mentionsReassigned\":%d}",
                        dedupedSourceIds.stream()
                                .map(id -> "\"" + id + "\"")
                                .collect(Collectors.joining(",")),
                        refreshed.getId(),
                        targetMentionCountDelta)));

        return mapToDto(refreshed);
    }

    /**
     * Renumber every cluster in {@code workspaceId} contiguously starting at 1,
     * preserving the existing cluster_number ordering.
     *
     * <p>
     * The unique index on {@code (workspace_id, cluster_number)} forbids ever
     * having two rows with the same number simultaneously. To rewrite numbers
     * safely we use a two-phase flush: first stamp temporary negative numbers
     * (which can never collide with positives), then assign the final positives.
     *
     * <p>
     * If numbers are already contiguous (1, 2, 3, ...) this is a no-op.
     */
    @Transactional
    public void compactClusterNumbers(@NonNull UUID workspaceId) {
        List<ClusterEntity> clusters =
                clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId);
        if (clusters.isEmpty()) {
            return;
        }

        // No-op fast path: already 1..N contiguous.
        boolean alreadyContiguous = true;
        for (int i = 0; i < clusters.size(); i++) {
            Integer current = clusters.get(i).getClusterNumber();
            if (current == null || current != i + 1) {
                alreadyContiguous = false;
                break;
            }
        }
        if (alreadyContiguous) {
            return;
        }

        // Phase 1: stamp negative temp numbers (cannot collide with positives).
        for (int i = 0; i < clusters.size(); i++) {
            clusters.get(i).setClusterNumber(-(i + 1));
        }
        clusterRepository.saveAllAndFlush(clusters);

        // Phase 2: assign final 1..N numbers.
        for (int i = 0; i < clusters.size(); i++) {
            clusters.get(i).setClusterNumber(i + 1);
        }
        clusterRepository.saveAllAndFlush(clusters);
    }

    /**
     * Update mention count cache for a cluster.
     */
    @Transactional
    public void updateMentionCount(@NonNull UUID clusterId) {
        ClusterEntity cluster = findClusterById(clusterId);
        long count = mentionRepository.countByClusterId(clusterId);
        cluster.setMentionCount((int) count);
        clusterRepository.save(cluster);
    }

    /**
     * Set representative text for cluster.
     */
    @Transactional
    public ClusterDto setRepresentativeText(@NonNull UUID clusterId,
            String text,
            @NonNull UUID callerId) {
        ClusterEntity cluster = findClusterById(clusterId);
        accessControl.requireMember(cluster.getWorkspaceId(), callerId);
        cluster.setRepresentativeText(text);
        ClusterEntity saved = clusterRepository.save(cluster);
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, saved.getWorkspaceId()));
        return mapToDto(saved);
    }

    private ClusterEntity findClusterById(UUID clusterId) {
        return clusterRepository.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cluster", clusterId));
    }

    private String getDefaultColor(int clusterNumber) {
        return DEFAULT_COLORS[(clusterNumber - 1) % DEFAULT_COLORS.length];
    }

    private ClusterDto mapToDto(ClusterEntity entity) {
        ClusterDto dto = new ClusterDto();
        dto.setId(entity.getId());
        dto.setWorkspaceId(entity.getWorkspaceId());
        dto.setClusterNumber(entity.getClusterNumber());
        dto.setLabel(entity.getLabel());
        dto.setRepresentativeText(entity.getRepresentativeText());
        dto.setColor(entity.getColor());
        dto.setMentionCount(entity.getMentionCount());
        return dto;
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // The IDOR fix already rejected the request at the requireMember
            // gate above any call into the service. If we somehow reach this
            // helper unauthenticated the audit log must NOT silently record
            // "system" — fail loud so the audit trail stays honest.
            throw new com.genesis.common.exception.UnauthorizedException("User not authenticated");
        }
        return auth.getName();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
