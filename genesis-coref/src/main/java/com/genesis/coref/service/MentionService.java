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

    public MentionService(MentionRepository mentionRepository,
            ClusterRepository clusterRepository,
            ClusterService clusterService) {
        this.mentionRepository = mentionRepository;
        this.clusterRepository = clusterRepository;
        this.clusterService = clusterService;
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

        return mapToDto(saved);
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
