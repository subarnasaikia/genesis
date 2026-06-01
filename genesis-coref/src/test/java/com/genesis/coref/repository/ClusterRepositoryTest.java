package com.genesis.coref.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.coref.entity.ClusterEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import com.genesis.coref.config.CorefTestConfiguration;

/**
 * Repository tests for ClusterRepository.
 */
@DataJpaTest
@ContextConfiguration(classes = CorefTestConfiguration.class)
class ClusterRepositoryTest {

    @Autowired
    private ClusterRepository clusterRepository;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should find clusters by workspace ordered by number")
    void findByWorkspaceIdOrderByClusterNumberAsc() {
        // Create clusters in reverse order
        createCluster(workspaceId, 2, "Second");
        createCluster(workspaceId, 1, "First");
        createCluster(workspaceId, 3, "Third");

        List<ClusterEntity> result = clusterRepository.findByWorkspaceIdOrderByClusterNumberAsc(workspaceId);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getClusterNumber());
        assertEquals(2, result.get(1).getClusterNumber());
        assertEquals(3, result.get(2).getClusterNumber());
    }

    @Test
    @DisplayName("Should find cluster by workspace and number")
    void findByWorkspaceIdAndClusterNumber() {
        createCluster(workspaceId, 1, "Test");

        Optional<ClusterEntity> result = clusterRepository
                .findByWorkspaceIdAndClusterNumber(workspaceId, 1);

        assertTrue(result.isPresent());
        assertEquals("Test", result.get().getLabel());
    }

    @Test
    @DisplayName("Should get next cluster number")
    void getNextClusterNumber() {
        createCluster(workspaceId, 1, "First");
        createCluster(workspaceId, 2, "Second");

        Integer next = clusterRepository.getNextClusterNumber(workspaceId);

        assertEquals(3, next);
    }

    @Test
    @DisplayName("Should return 1 for empty workspace")
    void getNextClusterNumberEmpty() {
        Integer next = clusterRepository.getNextClusterNumber(workspaceId);

        assertEquals(1, next);
    }

    @Test
    @DisplayName("Should count clusters by workspace")
    void countByWorkspaceId() {
        createCluster(workspaceId, 1, "One");
        createCluster(workspaceId, 2, "Two");
        createCluster(UUID.randomUUID(), 1, "Other"); // Different workspace

        long count = clusterRepository.countByWorkspaceId(workspaceId);

        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should check cluster existence")
    void existsByWorkspaceIdAndClusterNumber() {
        createCluster(workspaceId, 1, "Test");

        assertTrue(clusterRepository.existsByWorkspaceIdAndClusterNumber(workspaceId, 1));
        assertFalse(clusterRepository.existsByWorkspaceIdAndClusterNumber(workspaceId, 99));
    }

    @Test
    @DisplayName("Keyset first page returns lowest cluster numbers in order")
    void findPageByWorkspaceIdFirstPage() {
        createCluster(workspaceId, 3, "Third");
        createCluster(workspaceId, 1, "First");
        createCluster(workspaceId, 2, "Second");
        createCluster(UUID.randomUUID(), 1, "Other workspace"); // must be excluded

        List<ClusterEntity> page = clusterRepository.findPageByWorkspaceId(
                workspaceId, null, PageRequest.of(0, 2));

        assertEquals(2, page.size());
        assertEquals(1, page.get(0).getClusterNumber());
        assertEquals(2, page.get(1).getClusterNumber());
    }

    @Test
    @DisplayName("Keyset page after a cursor skips clusters at or below it")
    void findPageByWorkspaceIdAfterCursor() {
        createCluster(workspaceId, 1, "First");
        createCluster(workspaceId, 2, "Second");
        createCluster(workspaceId, 3, "Third");

        List<ClusterEntity> page = clusterRepository.findPageByWorkspaceId(
                workspaceId, 2, PageRequest.of(0, 10));

        assertEquals(1, page.size());
        assertEquals(3, page.get(0).getClusterNumber());
    }

    private ClusterEntity createCluster(UUID wsId, int number, String label) {
        ClusterEntity cluster = new ClusterEntity();
        cluster.setWorkspaceId(wsId);
        cluster.setClusterNumber(number);
        cluster.setLabel(label);
        cluster.setColor("#FF0000");
        cluster.setMentionCount(0);
        return clusterRepository.save(cluster);
    }
}
