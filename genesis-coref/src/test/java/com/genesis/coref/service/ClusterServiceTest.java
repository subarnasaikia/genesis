package com.genesis.coref.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.genesis.coref.dto.ClusterDto;
import com.genesis.coref.dto.CreateClusterRequest;
import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // After delete, compaction queries the remaining clusters; return empty.
        when(clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId))
                .thenReturn(Collections.emptyList());

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

    // ==================== Merge tests ====================

    @Test
    @DisplayName("mergeClusters happy path: reassigns mentions, sums counts, deletes sources, compacts numbers")
    void mergeClusters_happyPath() {
        UUID source1Id = UUID.randomUUID();
        UUID source2Id = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        ClusterEntity source1 = createClusterEntity(1, "S1");
        source1.setId(source1Id);
        source1.setMentionCount(2);

        ClusterEntity source2 = createClusterEntity(2, "S2");
        source2.setId(source2Id);
        source2.setMentionCount(3);

        ClusterEntity target = createClusterEntity(3, "T");
        target.setId(targetId);
        target.setMentionCount(4);

        Map<UUID, ClusterEntity> store = new HashMap<>();
        store.put(source1Id, source1);
        store.put(source2Id, source2);
        store.put(targetId, target);

        when(clusterRepository.findById(any(UUID.class)))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.<UUID>getArgument(0))));
        when(clusterRepository.save(any(ClusterEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // After deletion of source1+source2, the workspace ordering is target alone
        // — but its number is currently 3, which is non-contiguous, so compaction
        // will renumber it to 1. Return a mutable list so the service can mutate
        // cluster numbers in place.
        List<ClusterEntity> remaining = new ArrayList<>();
        remaining.add(target);
        when(clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId))
                .thenReturn(remaining);
        when(clusterRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ClusterDto result = clusterService.mergeClusters(
                workspaceId,
                Arrays.asList(source1Id, source2Id),
                targetId);

        // Mentions reassigned via single batch UPDATE.
        verify(mentionRepository).reassignMentionsToCluster(eq(targetId), anyList());

        // Source clusters deleted (deleteAll with both sources).
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Iterable<ClusterEntity>> deleteCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(clusterRepository).deleteAll(deleteCaptor.capture());
        List<ClusterEntity> deleted = new ArrayList<>();
        deleteCaptor.getValue().forEach(deleted::add);
        assertEquals(2, deleted.size());

        // Target's mention count = 4 (existing) + 2 + 3 = 9.
        assertEquals(9, target.getMentionCount());

        // Compaction rewrote the remaining cluster's number from 3 → 1.
        assertEquals(1, target.getClusterNumber());
        // Returned DTO reflects the renumber.
        assertEquals(1, result.getClusterNumber());
        assertEquals(targetId, result.getId());
    }

    @Test
    @DisplayName("mergeClusters rejects self-merge (target appears in sources)")
    void mergeClusters_selfMergeRejected() {
        UUID targetId = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> clusterService.mergeClusters(
                workspaceId, Arrays.asList(targetId, UUID.randomUUID()), targetId));
    }

    @Test
    @DisplayName("mergeClusters rejects when source is in a different workspace")
    void mergeClusters_crossWorkspaceRejected() {
        UUID otherWorkspaceId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        ClusterEntity target = createClusterEntity(1, "T");
        target.setId(targetId);

        ClusterEntity source = new ClusterEntity();
        source.setId(sourceId);
        source.setWorkspaceId(otherWorkspaceId);
        source.setClusterNumber(1);
        source.setMentionCount(1);

        when(clusterRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(clusterRepository.findById(sourceId)).thenReturn(Optional.of(source));

        assertThrows(ValidationException.class, () -> clusterService.mergeClusters(
                workspaceId, Collections.singletonList(sourceId), targetId));
    }

    @Test
    @DisplayName("mergeClusters throws ResourceNotFoundException when a cluster id is missing")
    void mergeClusters_missingClusterRejected() {
        UUID missingId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(clusterRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clusterService.mergeClusters(
                workspaceId, Collections.singletonList(missingId), targetId));
    }

    // ==================== Compaction tests ====================

    @Test
    @DisplayName("compactClusterNumbers fills gaps left by a deletion (1,2,4,5 -> 1,2,3,4)")
    void compactClusterNumbers_fillsGaps() {
        ClusterEntity c1 = createClusterEntity(1, "A");
        ClusterEntity c2 = createClusterEntity(2, "B");
        ClusterEntity c4 = createClusterEntity(4, "D");
        ClusterEntity c5 = createClusterEntity(5, "E");
        List<ClusterEntity> remaining = new ArrayList<>(Arrays.asList(c1, c2, c4, c5));

        when(clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId))
                .thenReturn(remaining);
        when(clusterRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));

        clusterService.compactClusterNumbers(workspaceId);

        // After two-phase compaction, numbers are 1, 2, 3, 4 in order.
        assertEquals(1, c1.getClusterNumber());
        assertEquals(2, c2.getClusterNumber());
        assertEquals(3, c4.getClusterNumber());
        assertEquals(4, c5.getClusterNumber());

        // Two flushes: phase 1 (negatives) + phase 2 (final positives).
        verify(clusterRepository, times(2)).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("compactClusterNumbers is a no-op when numbers are already contiguous")
    void compactClusterNumbers_noOpWhenContiguous() {
        ClusterEntity c1 = createClusterEntity(1, "A");
        ClusterEntity c2 = createClusterEntity(2, "B");
        ClusterEntity c3 = createClusterEntity(3, "C");
        when(clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId))
                .thenReturn(Arrays.asList(c1, c2, c3));

        clusterService.compactClusterNumbers(workspaceId);

        verify(clusterRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("deleteCluster automatically compacts remaining cluster numbers")
    void deleteCluster_compactsNumbers() {
        // Simulate: workspace had clusters 1,2,3; we delete cluster 2. After delete,
        // findByWorkspaceIdOrderByClusterNumberAsc returns the remaining clusters
        // numbered (1, 3) — compaction should rewrite to (1, 2).
        UUID toDeleteId = UUID.randomUUID();
        ClusterEntity toDelete = createClusterEntity(2, "Mid");
        toDelete.setId(toDeleteId);

        ClusterEntity remaining1 = createClusterEntity(1, "First");
        ClusterEntity remaining3 = createClusterEntity(3, "Third");
        List<ClusterEntity> remaining = new ArrayList<>(Arrays.asList(remaining1, remaining3));

        when(clusterRepository.findById(toDeleteId)).thenReturn(Optional.of(toDelete));
        when(clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId))
                .thenReturn(remaining);
        when(clusterRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));

        clusterService.deleteCluster(toDeleteId);

        // After compaction: 1 stays 1, 3 becomes 2.
        assertEquals(1, remaining1.getClusterNumber());
        assertEquals(2, remaining3.getClusterNumber());
        verify(clusterRepository).delete(toDelete);
        verify(mentionRepository).unassignFromCluster(toDeleteId);
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
