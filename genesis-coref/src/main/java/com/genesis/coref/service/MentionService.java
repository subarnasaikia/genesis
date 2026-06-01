package com.genesis.coref.service;

import com.genesis.coref.dto.CreateMentionRequest;
import com.genesis.coref.dto.MentionDto;
import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.event.ActionType;
import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.common.event.MentionAnnotatedEvent;
import com.genesis.common.event.WorkspaceActivityEvent;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import com.genesis.common.response.CursorPage;
import com.genesis.workspace.service.WorkspaceAccessControl;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mention operations.
 */
@Service
public class MentionService {

    private static final Logger logger = LoggerFactory.getLogger(MentionService.class);

    private final MentionRepository mentionRepository;
    private final ClusterRepository clusterRepository;
    private final ClusterService clusterService;
    private final WorkspaceAccessControl accessControl;
    private final ApplicationEventPublisher eventPublisher;

    public MentionService(MentionRepository mentionRepository,
            ClusterRepository clusterRepository,
            ClusterService clusterService,
            WorkspaceAccessControl accessControl,
            ApplicationEventPublisher eventPublisher) {
        this.mentionRepository = mentionRepository;
        this.clusterRepository = clusterRepository;
        this.clusterService = clusterService;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new mention.
     */
    @Transactional
    public MentionDto createMention(@NonNull UUID workspaceId,
            @NonNull CreateMentionRequest request,
            @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        // Validate no overlap with existing mentions
        boolean hasOverlap = mentionRepository.hasOverlappingMention(
                request.getDocumentId(),
                request.getSentenceIndex(),
                request.getStartTokenIndex(),
                request.getEndTokenIndex());
        if (hasOverlap) {
            throw new ValidationException("Mention overlaps with existing mention in this sentence");
        }

        MentionEntity mention = new MentionEntity();
        mention.setWorkspaceId(workspaceId);
        mention.setDocumentId(request.getDocumentId());
        mention.setSentenceIndex(request.getSentenceIndex());
        mention.setStartTokenIndex(request.getStartTokenIndex());
        mention.setEndTokenIndex(request.getEndTokenIndex());
        mention.setText(request.getText());
        mention.setMentionType(request.getMentionType());
        mention.setClusterId(request.getClusterId());

        MentionEntity saved = mentionRepository.save(mention);

        // Update cluster mention count if assigned
        if (saved.getClusterId() != null) {
            clusterService.updateMentionCount(saved.getClusterId());
        }

        // Update document progress and status
        publishMentionAnnotated(saved.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));

        // Audit log
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                workspaceId,
                currentUser(),
                ActionType.MENTION_CREATED,
                saved.getId(),
                String.format("{\"documentId\":\"%s\",\"clusterId\":%s}",
                        saved.getDocumentId(),
                        saved.getClusterId() == null ? "null" : "\"" + saved.getClusterId() + "\"")));

        return mapToDto(saved);
    }

    /**
     * Signal that a document's mentions changed so genesis-workspace can
     * recompute the document's status/progress on its own entity. We publish the
     * total mention-token count (coref's own datum); the workspace listener owns
     * the document state machine and the progress formula (ARCHITECTURE_AUDIT
     * A-001). Sourcing the count here keeps the cross-module event thin.
     */
    private void publishMentionAnnotated(UUID documentId) {
        Long mentionTokens = mentionRepository.sumMentionTokensByDocumentId(documentId);
        eventPublisher.publishEvent(new MentionAnnotatedEvent(
                this, documentId, mentionTokens == null ? 0L : mentionTokens));
    }

    /**
     * Get a keyset page of mentions for a workspace, ordered by primary key.
     *
     * @param workspaceId the workspace
     * @param callerId    the authenticated caller (must be a member)
     * @param cursor      the last id from the previous page, or {@code null} for
     *                    the first page
     * @param limit       the requested page size (clamped by {@link CursorPage})
     * @return a page of mentions plus the cursor for the next page
     */
    public CursorPage<MentionDto> getMentionsByWorkspace(@NonNull UUID workspaceId, @NonNull UUID callerId,
            UUID cursor, int limit) {
        accessControl.requireMember(workspaceId, callerId);
        int pageSize = CursorPage.clampLimit(limit);
        // Fetch one extra row to learn whether another page exists without a count.
        List<MentionEntity> rows = mentionRepository.findPageByWorkspaceId(
                workspaceId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<MentionEntity> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<MentionDto> items = pageRows.stream().map(this::mapToDto).collect(Collectors.toList());
        String nextCursor = hasMore ? pageRows.get(pageRows.size() - 1).getId().toString() : null;
        return CursorPage.of(items, nextCursor, pageSize, hasMore);
    }

    /**
     * Get all mentions for a document.
     */
    public List<MentionDto> getMentionsByDocument(@NonNull UUID workspaceId, @NonNull UUID documentId,
            @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        return mentionRepository.findByDocumentIdOrdered(documentId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all mentions in a cluster.
     */
    public List<MentionDto> getMentionsByCluster(@NonNull UUID clusterId, @NonNull UUID callerId) {
        UUID workspaceId = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cluster", clusterId))
                .getWorkspaceId();
        accessControl.requireMember(workspaceId, callerId);
        return mentionRepository.findByClusterIdOrdered(clusterId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get mention by ID.
     */
    public MentionDto getMention(@NonNull UUID mentionId, @NonNull UUID callerId) {
        MentionEntity mention = findMentionById(mentionId);
        accessControl.requireMember(mention.getWorkspaceId(), callerId);
        return mapToDto(mention);
    }

    /**
     * Assign mention to cluster.
     */
    @Transactional
    public MentionDto assignToCluster(@NonNull UUID mentionId,
            @NonNull UUID clusterId,
            @NonNull UUID callerId) {
        MentionEntity mention = findMentionById(mentionId);
        accessControl.requireMember(mention.getWorkspaceId(), callerId);
        UUID oldClusterId = mention.getClusterId();

        // Verify cluster exists
        if (!clusterRepository.existsById(clusterId)) {
            throw new ResourceNotFoundException("Cluster", clusterId);
        }

        mention.setClusterId(clusterId);
        MentionEntity saved = mentionRepository.save(mention);

        // Update mention counts
        if (oldClusterId != null) {
            clusterService.updateMentionCount(oldClusterId);
        }
        clusterService.updateMentionCount(clusterId);

        publishMentionAnnotated(saved.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, saved.getWorkspaceId()));

        // Audit log: mention assigned
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                saved.getWorkspaceId(),
                currentUser(),
                ActionType.MENTION_ASSIGNED,
                saved.getId(),
                String.format("{\"oldClusterId\":%s,\"newClusterId\":\"%s\"}",
                        oldClusterId == null ? "null" : "\"" + oldClusterId + "\"",
                        clusterId)));

        return mapToDto(saved);
    }

    /**
     * Unassign mention from cluster.
     */
    @Transactional
    public MentionDto unassignFromCluster(@NonNull UUID mentionId, @NonNull UUID callerId) {
        MentionEntity mention = findMentionById(mentionId);
        accessControl.requireMember(mention.getWorkspaceId(), callerId);
        UUID oldClusterId = mention.getClusterId();

        mention.setClusterId(null);
        MentionEntity saved = mentionRepository.save(mention);

        // Update mention count
        if (oldClusterId != null) {
            clusterService.updateMentionCount(oldClusterId);
        }

        publishMentionAnnotated(saved.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, saved.getWorkspaceId()));

        // Audit log: mention unassigned (recorded as MENTION_ASSIGNED with newClusterId=null)
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                saved.getWorkspaceId(),
                currentUser(),
                ActionType.MENTION_ASSIGNED,
                saved.getId(),
                String.format("{\"oldClusterId\":%s,\"newClusterId\":null}",
                        oldClusterId == null ? "null" : "\"" + oldClusterId + "\"")));

        return mapToDto(saved);
    }

    /**
     * Delete mention.
     */
    @Transactional
    public void deleteMention(@NonNull UUID mentionId, @NonNull UUID callerId) {
        MentionEntity mention = findMentionById(mentionId);
        accessControl.requireMember(mention.getWorkspaceId(), callerId);
        UUID clusterId = mention.getClusterId();

        mentionRepository.delete(mention);

        // Update cluster mention count
        if (clusterId != null) {
            clusterService.updateMentionCount(clusterId);
        }

        publishMentionAnnotated(mention.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, mention.getWorkspaceId()));

        // Audit log: mention deleted
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                mention.getWorkspaceId(),
                currentUser(),
                ActionType.MENTION_DELETED,
                mention.getId(),
                String.format("{\"clusterId\":%s,\"documentId\":\"%s\"}",
                        clusterId == null ? "null" : "\"" + clusterId + "\"",
                        mention.getDocumentId())));
    }

    /**
     * Get unassigned mentions.
     */
    public List<MentionDto> getUnassignedMentions(@NonNull UUID workspaceId, @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        return mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private MentionEntity findMentionById(UUID mentionId) {
        return mentionRepository.findById(mentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mention", mentionId));
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

    private MentionDto mapToDto(MentionEntity entity) {
        MentionDto dto = new MentionDto();
        dto.setId(entity.getId());
        dto.setWorkspaceId(entity.getWorkspaceId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setClusterId(entity.getClusterId());
        dto.setSentenceIndex(entity.getSentenceIndex());
        dto.setStartTokenIndex(entity.getStartTokenIndex());
        dto.setEndTokenIndex(entity.getEndTokenIndex());
        dto.setGlobalStartIndex(entity.getGlobalStartIndex());
        dto.setGlobalEndIndex(entity.getGlobalEndIndex());
        dto.setText(entity.getText());
        dto.setMentionType(entity.getMentionType());

        // Add cluster info if assigned
        if (entity.getClusterId() != null) {
            clusterRepository.findById(entity.getClusterId()).ifPresent(cluster -> {
                dto.setClusterNumber(cluster.getClusterNumber());
                dto.setClusterColor(cluster.getColor());
            });
        }

        return dto;
    }
}
