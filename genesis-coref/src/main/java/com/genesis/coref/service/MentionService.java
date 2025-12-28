package com.genesis.coref.service;

import com.genesis.coref.dto.CreateMentionRequest;
import com.genesis.coref.dto.MentionDto;
import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.genesis.common.event.WorkspaceActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mention operations.
 */
@Service
public class MentionService {

    private final MentionRepository mentionRepository;
    private final ClusterRepository clusterRepository;
    private final ClusterService clusterService;
    private final com.genesis.workspace.service.DocumentService documentService;
    private final ApplicationEventPublisher eventPublisher;

    public MentionService(MentionRepository mentionRepository,
            ClusterRepository clusterRepository,
            ClusterService clusterService,
            com.genesis.workspace.service.DocumentService documentService,
            ApplicationEventPublisher eventPublisher) {
        this.mentionRepository = mentionRepository;
        this.clusterRepository = clusterRepository;
        this.clusterService = clusterService;
        this.documentService = documentService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new mention.
     */
    @Transactional
    public MentionDto createMention(@NonNull UUID workspaceId, @NonNull CreateMentionRequest request) {
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
        updateDocumentProgress(saved.getDocumentId());

        updateDocumentProgress(saved.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));

        return mapToDto(saved);
    }

    /**
     * Update document progress and status.
     */
    private void updateDocumentProgress(UUID documentId) {
        try {
            var doc = documentService.getById(documentId);

            // Update status to ANNOTATING if needed
            if (doc.getStatus() == com.genesis.workspace.entity.DocumentStatus.UPLOADED ||
                    doc.getStatus() == com.genesis.workspace.entity.DocumentStatus.IMPORTED) {
                documentService.updateStatus(documentId, com.genesis.workspace.entity.DocumentStatus.ANNOTATING);
            }
            // Also if was complete but we are editing, maybe we should not revert?
            // User requirement: "when the annotation file file have at least one annotation
            // mentions either with cluster or in unassigned it should show as in progress"
            // So if we add a mention, it should be IN_PROGRESS (ANNOTATING).
            // But if user marked it COMPLETE, do we force it back?
            // "and for the annoted completed user should able to updated that"
            // If user explicitly marks completed, adding a mention might be a correction.
            // Let's stick to: If NEW/IMPORTED -> ANNOTATING.
            // If COMPLETE, maybe leave it? Or revert?
            // Implementation: Only change if UPLOADED or IMPORTED.
            // Wait, if I delete all mentions, should it go back? Probably not important.

            // Calculate progress
            long totalTokens = (long) (doc.getTokenEndIndex() - doc.getTokenStartIndex() + 1);
            long mentionTokens = mentionRepository.sumMentionTokensByDocumentId(documentId);

            Double progress = 0.0;
            if (totalTokens > 0) {
                progress = (double) mentionTokens / totalTokens;
                if (progress > 1.0)
                    progress = 1.0; // Cap at 100%
            }

            documentService.updateProgress(documentId, progress);

        } catch (Exception e) {
            // Log but don't fail the operation
            System.err.println("Failed to update document progress: " + e.getMessage());
        }
    }

    /**
     * Get all mentions for a workspace.
     */
    public List<MentionDto> getMentionsByWorkspace(@NonNull UUID workspaceId) {
        return mentionRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all mentions for a document.
     */
    public List<MentionDto> getMentionsByDocument(@NonNull UUID documentId) {
        return mentionRepository.findByDocumentIdOrdered(documentId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all mentions in a cluster.
     */
    public List<MentionDto> getMentionsByCluster(@NonNull UUID clusterId) {
        return mentionRepository.findByClusterIdOrdered(clusterId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get mention by ID.
     */
    public MentionDto getMention(@NonNull UUID mentionId) {
        MentionEntity mention = findMentionById(mentionId);
        return mapToDto(mention);
    }

    /**
     * Assign mention to cluster.
     */
    @Transactional
    public MentionDto assignToCluster(@NonNull UUID mentionId, @NonNull UUID clusterId) {
        MentionEntity mention = findMentionById(mentionId);
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

        updateDocumentProgress(saved.getDocumentId());

        updateDocumentProgress(saved.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, saved.getWorkspaceId()));

        return mapToDto(saved);
    }

    /**
     * Unassign mention from cluster.
     */
    @Transactional
    public MentionDto unassignFromCluster(@NonNull UUID mentionId) {
        MentionEntity mention = findMentionById(mentionId);
        UUID oldClusterId = mention.getClusterId();

        mention.setClusterId(null);
        MentionEntity saved = mentionRepository.save(mention);

        // Update mention count
        if (oldClusterId != null) {
            clusterService.updateMentionCount(oldClusterId);
        }

        updateDocumentProgress(saved.getDocumentId());

        updateDocumentProgress(saved.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, saved.getWorkspaceId()));

        return mapToDto(saved);
    }

    /**
     * Delete mention.
     */
    @Transactional
    public void deleteMention(@NonNull UUID mentionId) {
        MentionEntity mention = findMentionById(mentionId);
        UUID clusterId = mention.getClusterId();

        mentionRepository.delete(mention);

        // Update cluster mention count
        if (clusterId != null) {
            clusterService.updateMentionCount(clusterId);
        }

        updateDocumentProgress(mention.getDocumentId());

        // Publish workspace activity event
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, mention.getWorkspaceId()));
    }

    /**
     * Get unassigned mentions.
     */
    public List<MentionDto> getUnassignedMentions(@NonNull UUID workspaceId) {
        return mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private MentionEntity findMentionById(UUID mentionId) {
        return mentionRepository.findById(mentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mention", mentionId));
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
