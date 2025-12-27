package com.genesis.coref.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CoreferenceService.
 */
@ExtendWith(MockitoExtension.class)
class CoreferenceServiceTest {

    @Mock
    private MentionRepository mentionRepository;

    @Mock
    private ClusterRepository clusterRepository;

    private CoreferenceService coreferenceService;

    private UUID workspaceId;
    private UUID documentId;
    private UUID clusterId;

    @BeforeEach
    void setUp() {
        coreferenceService = new CoreferenceService(mentionRepository, clusterRepository);
        workspaceId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        clusterId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should generate single-token coref annotation")
    void generateSingleTokenAnnotation() {
        MentionEntity mention = createMention(0, 0, 0, clusterId);
        ClusterEntity cluster = createCluster(1);

        when(mentionRepository.findByDocumentIdOrdered(documentId))
                .thenReturn(Arrays.asList(mention));
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));

        Map<String, String> annotations = coreferenceService.generateCorefAnnotations(documentId);

        assertEquals("(1)", annotations.get("0-0"));
    }

    @Test
    @DisplayName("Should generate multi-token coref annotation")
    void generateMultiTokenAnnotation() {
        MentionEntity mention = createMention(0, 0, 2, clusterId);
        ClusterEntity cluster = createCluster(1);

        when(mentionRepository.findByDocumentIdOrdered(documentId))
                .thenReturn(Arrays.asList(mention));
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));

        Map<String, String> annotations = coreferenceService.generateCorefAnnotations(documentId);

        assertEquals("(1", annotations.get("0-0")); // Start
        assertEquals("1)", annotations.get("0-2")); // End
    }

    @Test
    @DisplayName("Should merge overlapping annotations with pipe")
    void mergeOverlappingAnnotations() {
        UUID cluster1Id = UUID.randomUUID();
        UUID cluster2Id = UUID.randomUUID();

        MentionEntity mention1 = createMention(0, 0, 0, cluster1Id);
        MentionEntity mention2 = createMention(0, 0, 0, cluster2Id);

        ClusterEntity cluster1 = createCluster(1);
        cluster1.setId(cluster1Id);
        ClusterEntity cluster2 = createCluster(2);
        cluster2.setId(cluster2Id);

        when(mentionRepository.findByDocumentIdOrdered(documentId))
                .thenReturn(Arrays.asList(mention1, mention2));
        when(clusterRepository.findById(cluster1Id)).thenReturn(Optional.of(cluster1));
        when(clusterRepository.findById(cluster2Id)).thenReturn(Optional.of(cluster2));

        Map<String, String> annotations = coreferenceService.generateCorefAnnotations(documentId);

        String annotation = annotations.get("0-0");
        assertTrue(annotation.contains("|"));
        assertTrue(annotation.contains("(1)"));
        assertTrue(annotation.contains("(2)"));
    }

    @Test
    @DisplayName("Should skip unassigned mentions")
    void skipUnassignedMentions() {
        MentionEntity mention = createMention(0, 0, 0, null); // No cluster

        when(mentionRepository.findByDocumentIdOrdered(documentId))
                .thenReturn(Arrays.asList(mention));

        Map<String, String> annotations = coreferenceService.generateCorefAnnotations(documentId);

        assertTrue(annotations.isEmpty());
    }

    @Test
    @DisplayName("Should generate workspace-level annotations")
    void generateWorkspaceAnnotations() {
        UUID doc1 = UUID.randomUUID();
        UUID doc2 = UUID.randomUUID();

        MentionEntity mention1 = createMention(0, 0, 0, clusterId);
        mention1.setDocumentId(doc1);
        MentionEntity mention2 = createMention(0, 1, 1, clusterId);
        mention2.setDocumentId(doc2);

        ClusterEntity cluster = createCluster(1);

        when(mentionRepository.findByWorkspaceId(workspaceId))
                .thenReturn(Arrays.asList(mention1, mention2));
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));

        Map<UUID, Map<String, String>> annotations = coreferenceService.generateWorkspaceCorefAnnotations(workspaceId);

        assertEquals(2, annotations.size());
        assertTrue(annotations.containsKey(doc1));
        assertTrue(annotations.containsKey(doc2));
    }

    @Test
    @DisplayName("Should get annotation stats")
    void getStats() {
        when(mentionRepository.countByWorkspaceId(workspaceId)).thenReturn(10L);
        when(clusterRepository.countByWorkspaceId(workspaceId)).thenReturn(3L);
        when(mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId))
                .thenReturn(Arrays.asList(new MentionEntity(), new MentionEntity()));

        CoreferenceService.AnnotationStats stats = coreferenceService.getStats(workspaceId);

        assertEquals(10, stats.getMentionCount());
        assertEquals(3, stats.getClusterCount());
        assertEquals(2, stats.getUnassignedCount());
    }

    private MentionEntity createMention(int sentenceIdx, int startToken, int endToken, UUID clusterId) {
        MentionEntity entity = new MentionEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setDocumentId(documentId);
        entity.setSentenceIndex(sentenceIdx);
        entity.setStartTokenIndex(startToken);
        entity.setEndTokenIndex(endToken);
        entity.setClusterId(clusterId);
        return entity;
    }

    private ClusterEntity createCluster(int number) {
        ClusterEntity entity = new ClusterEntity();
        entity.setId(clusterId);
        entity.setWorkspaceId(workspaceId);
        entity.setClusterNumber(number);
        return entity;
    }
}
