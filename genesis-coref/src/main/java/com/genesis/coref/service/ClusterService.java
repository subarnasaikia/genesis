package com.genesis.coref.service;

import com.genesis.coref.dto.ClusterDto;
import com.genesis.coref.dto.CreateClusterRequest;
import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
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

    public ClusterService(ClusterRepository clusterRepository,
            MentionRepository mentionRepository) {
        this.clusterRepository = clusterRepository;
        this.mentionRepository = mentionRepository;
    }

    /**
     * Create a new cluster.
     */
    @Transactional
    public ClusterDto createCluster(@NonNull UUID workspaceId, CreateClusterRequest request) {
        Integer nextNumber = clusterRepository.getNextClusterNumber(workspaceId);

        ClusterEntity cluster = new ClusterEntity();
        cluster.setWorkspaceId(workspaceId);
        cluster.setClusterNumber(nextNumber);
        cluster.setLabel(request != null ? request.getLabel() : null);
        cluster.setColor(
                request != null && request.getColor() != null ? request.getColor() : getDefaultColor(nextNumber));
        cluster.setMentionCount(0);

        ClusterEntity saved = clusterRepository.save(cluster);
        return mapToDto(saved);
    }

    /**
     * Get all clusters for a workspace.
     */
    public List<ClusterDto> getClusters(@NonNull UUID workspaceId) {
        return clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get cluster by ID.
     */
    public ClusterDto getCluster(@NonNull UUID clusterId) {
        ClusterEntity cluster = findClusterById(clusterId);
        return mapToDto(cluster);
    }

    /**
     * Update cluster.
     */
    @Transactional
    public ClusterDto updateCluster(@NonNull UUID clusterId, CreateClusterRequest request) {
        ClusterEntity cluster = findClusterById(clusterId);

        if (request.getLabel() != null) {
            cluster.setLabel(request.getLabel());
        }
        if (request.getColor() != null) {
            cluster.setColor(request.getColor());
        }

        ClusterEntity saved = clusterRepository.save(cluster);
        return mapToDto(saved);
    }

    /**
     * Delete cluster. Mentions are unassigned (not deleted).
     */
    @Transactional
    public void deleteCluster(@NonNull UUID clusterId) {
        ClusterEntity cluster = findClusterById(clusterId);

        // Unassign all mentions from this cluster
        mentionRepository.unassignFromCluster(clusterId);

        clusterRepository.delete(cluster);
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
    public ClusterDto setRepresentativeText(@NonNull UUID clusterId, String text) {
        ClusterEntity cluster = findClusterById(clusterId);
        cluster.setRepresentativeText(text);
        ClusterEntity saved = clusterRepository.save(cluster);
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
}
