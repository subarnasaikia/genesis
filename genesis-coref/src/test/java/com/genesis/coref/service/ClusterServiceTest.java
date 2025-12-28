package com.genesis.coref.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.genesis.coref.dto.ClusterDto;
import com.genesis.coref.dto.CreateClusterRequest;
import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ClusterService.
 */
@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private MentionRepository mentionRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private ClusterService clusterService;

    private UUID workspaceId;
    private UUID clusterId;

    @BeforeEach
    void setUp() {
        clusterService = new ClusterService(clusterRepository, mentionRepository, eventPublisher);
        workspaceId = UUID.randomUUID();
        clusterId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create cluster with auto-generated number")
    void createCluster() {
        when(clusterRepository.getNextClusterNumber(workspaceId)).thenReturn(1);
        when(clusterRepository.save(any(ClusterEntity.class))).thenAnswer(inv -> {
            ClusterEntity entity = inv.getArgument(0);
            entity.setId(clusterId);
            return entity;
        });

        CreateClusterRequest request = new CreateClusterRequest();
        request.setLabel("Test Cluster");

        ClusterDto result = clusterService.createCluster(workspaceId, request);

        assertNotNull(result);
        assertEquals(1, result.getClusterNumber());
        assertEquals("Test Cluster", result.getLabel());
        assertNotNull(result.getColor()); // Auto-assigned color
    }

    @Test
    @DisplayName("Should create cluster with custom color")
    void createClusterWithColor() {
        when(clusterRepository.getNextClusterNumber(workspaceId)).thenReturn(1);
        when(clusterRepository.save(any(ClusterEntity.class))).thenAnswer(inv -> {
            ClusterEntity entity = inv.getArgument(0);
            entity.setId(clusterId);
            return entity;
        });

        CreateClusterRequest request = new CreateClusterRequest();
        request.setColor("#123456");

        ClusterDto result = clusterService.createCluster(workspaceId, request);

        assertEquals("#123456", result.getColor());
    }

    @Test
    @DisplayName("Should get all clusters for workspace")
    void getClusters() {
        ClusterEntity cluster1 = createClusterEntity(1, "Cluster 1");
        ClusterEntity cluster2 = createClusterEntity(2, "Cluster 2");

        when(clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId))
                .thenReturn(Arrays.asList(cluster1, cluster2));

        List<ClusterDto> result = clusterService.getClusters(workspaceId);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getClusterNumber());
        assertEquals(2, result.get(1).getClusterNumber());
    }

    @Test
    @DisplayName("Should update cluster label and color")
    void updateCluster() {
        ClusterEntity existing = createClusterEntity(1, "Old Label");
        existing.setId(clusterId);

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        when(clusterRepository.save(any(ClusterEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateClusterRequest request = new CreateClusterRequest();
        request.setLabel("New Label");
        request.setColor("#ABCDEF");

        ClusterDto result = clusterService.updateCluster(clusterId, request);

        assertEquals("New Label", result.getLabel());
        assertEquals("#ABCDEF", result.getColor());
    }

    @Test
    @DisplayName("Should throw when cluster not found")
    void throwWhenClusterNotFound() {
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clusterService.getCluster(clusterId));
    }

    @Test
    @DisplayName("Should delete cluster and unassign mentions")
    void deleteCluster() {
        ClusterEntity existing = createClusterEntity(1, "To Delete");
        existing.setId(clusterId);

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));

        clusterService.deleteCluster(clusterId);

        verify(mentionRepository).unassignFromCluster(clusterId);
        verify(clusterRepository).delete(existing);
    }

    @Test
    @DisplayName("Should update mention count")
    void updateMentionCount() {
        ClusterEntity existing = createClusterEntity(1, "Test");
        existing.setId(clusterId);

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        when(mentionRepository.countByClusterId(clusterId)).thenReturn(5L);
        when(clusterRepository.save(any(ClusterEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        clusterService.updateMentionCount(clusterId);

        ArgumentCaptor<ClusterEntity> captor = ArgumentCaptor.forClass(ClusterEntity.class);
        verify(clusterRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getMentionCount());
    }

    private ClusterEntity createClusterEntity(int number, String label) {
        ClusterEntity entity = new ClusterEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setClusterNumber(number);
        entity.setLabel(label);
        entity.setColor("#FF6B6B");
        entity.setMentionCount(0);
        return entity;
    }
}
