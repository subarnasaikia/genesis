package com.genesis.coref.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.entity.MentionEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import com.genesis.coref.config.CorefTestConfiguration;

/**
 * Repository tests for MentionRepository.
 */
@DataJpaTest
@ContextConfiguration(classes = CorefTestConfiguration.class)
class MentionRepositoryTest {

    @Autowired
    private MentionRepository mentionRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    private UUID workspaceId;
    private UUID documentId;
    private UUID clusterId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        ClusterEntity cluster = new ClusterEntity();
        cluster.setWorkspaceId(workspaceId);
        cluster.setClusterNumber(1);
        cluster.setLabel("Test");
        cluster.setColor("#FF0000");
        cluster.setMentionCount(0);
        clusterId = clusterRepository.save(cluster).getId();
    }

    @Test
    @DisplayName("Should find mentions by document ordered")
    void findByDocumentIdOrdered() {
        createMention(1, 0, 2);
        createMention(0, 0, 1);
        createMention(0, 3, 4);

        List<MentionEntity> result = mentionRepository.findByDocumentIdOrdered(documentId);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getSentenceIndex());
        assertEquals(0, result.get(0).getStartTokenIndex());
        assertEquals(3, result.get(1).getStartTokenIndex());
        assertEquals(1, result.get(2).getSentenceIndex());
    }

    @Test
    @DisplayName("Should find mentions by cluster")
    void findByClusterId() {
        MentionEntity m1 = createMention(0, 0, 1);
        m1.setClusterId(clusterId);
        mentionRepository.save(m1);

        MentionEntity m2 = createMention(0, 2, 3);
        // No cluster

        List<MentionEntity> result = mentionRepository.findByClusterId(clusterId);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should find unassigned mentions")
    void findByWorkspaceIdAndClusterIdIsNull() {
        MentionEntity m1 = createMention(0, 0, 1);
        m1.setClusterId(clusterId);
        mentionRepository.save(m1);

        createMention(0, 2, 3); // Unassigned

        List<MentionEntity> result = mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId);

        assertEquals(1, result.size());
        assertNull(result.get(0).getClusterId());
    }

    @Test
    @DisplayName("Should detect overlapping mentions")
    void hasOverlappingMention() {
        createMention(0, 2, 5);

        // Overlaps (3-4 inside 2-5)
        assertTrue(mentionRepository.hasOverlappingMention(documentId, 0, 3, 4));

        // Overlaps (1-3 overlaps start)
        assertTrue(mentionRepository.hasOverlappingMention(documentId, 0, 1, 3));

        // Overlaps (4-7 overlaps end)
        assertTrue(mentionRepository.hasOverlappingMention(documentId, 0, 4, 7));

        // No overlap (0-1 before)
        assertFalse(mentionRepository.hasOverlappingMention(documentId, 0, 0, 1));

        // No overlap (6-8 after)
        assertFalse(mentionRepository.hasOverlappingMention(documentId, 0, 6, 8));

        // Different sentence
        assertFalse(mentionRepository.hasOverlappingMention(documentId, 1, 2, 5));
    }

    @Test
    @DisplayName("Should count mentions by cluster")
    void countByClusterId() {
        MentionEntity m1 = createMention(0, 0, 1);
        m1.setClusterId(clusterId);
        mentionRepository.save(m1);

        MentionEntity m2 = createMention(0, 2, 3);
        m2.setClusterId(clusterId);
        mentionRepository.save(m2);

        assertEquals(2, mentionRepository.countByClusterId(clusterId));
    }

    @Test
    @DisplayName("Should unassign mentions from cluster")
    void unassignFromCluster() {
        MentionEntity m1 = createMention(0, 0, 1);
        m1.setClusterId(clusterId);
        mentionRepository.save(m1);

        mentionRepository.unassignFromCluster(clusterId);

        List<MentionEntity> result = mentionRepository.findByClusterId(clusterId);
        assertTrue(result.isEmpty());
    }

    private MentionEntity createMention(int sentenceIdx, int startToken, int endToken) {
        MentionEntity mention = new MentionEntity();
        mention.setWorkspaceId(workspaceId);
        mention.setDocumentId(documentId);
        mention.setSentenceIndex(sentenceIdx);
        mention.setStartTokenIndex(startToken);
        mention.setEndTokenIndex(endToken);
        mention.setText("Test");
        return mentionRepository.save(mention);
    }
}
