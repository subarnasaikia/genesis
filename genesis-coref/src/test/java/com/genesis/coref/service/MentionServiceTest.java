package com.genesis.coref.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.genesis.coref.dto.CreateMentionRequest;
import com.genesis.coref.dto.MentionDto;
import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import com.genesis.common.response.CursorPage;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for MentionService.
 */
@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    @Mock
    private MentionRepository mentionRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.genesis.workspace.service.WorkspaceAccessControl accessControl;

    private ClusterService clusterService; // Real service, not mocked
    private MentionService mentionService;

    private UUID workspaceId;
    private UUID documentId;
    private UUID mentionId;
    private UUID clusterId;
    private UUID callerId;

    @BeforeEach
    void setUp() {
        // Use real ClusterService with mocked repos to avoid Java 25 Mockito issues
        clusterService = new ClusterService(clusterRepository, mentionRepository, accessControl, eventPublisher);
        mentionService = new MentionService(mentionRepository, clusterRepository, clusterService,
                accessControl, eventPublisher);
        workspaceId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        mentionId = UUID.randomUUID();
        clusterId = UUID.randomUUID();
        callerId = UUID.randomUUID();

        // currentUser() reads SecurityContextHolder for the audit log. Seed a
        // bare-bones authenticated principal so the helper does not throw.
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "test-user", "n/a", java.util.Collections.emptyList()));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create mention")
    void createMention() {
        when(mentionRepository.hasOverlappingMention(any(), anyInt(), anyInt(), anyInt())).thenReturn(false);
        when(mentionRepository.save(any(MentionEntity.class))).thenAnswer(inv -> {
            MentionEntity entity = inv.getArgument(0);
            entity.setId(mentionId);
            return entity;
        });

        CreateMentionRequest request = new CreateMentionRequest();
        request.setDocumentId(documentId);
        request.setSentenceIndex(0);
        request.setStartTokenIndex(0);
        request.setEndTokenIndex(2);
        request.setText("Test mention");

        MentionDto result = mentionService.createMention(workspaceId, request, callerId);

        assertNotNull(result);
        assertEquals(workspaceId, result.getWorkspaceId());
        assertEquals(0, result.getSentenceIndex());
        assertEquals(0, result.getStartTokenIndex());
        assertEquals(2, result.getEndTokenIndex());
        // Progress/status is now driven by an event consumed in genesis-workspace (A-001).
        verify(eventPublisher).publishEvent(any(com.genesis.common.event.MentionAnnotatedEvent.class));
    }

    @Test
    @DisplayName("Should throw when mention overlaps")
    void throwOnOverlap() {
        when(mentionRepository.hasOverlappingMention(any(), anyInt(), anyInt(), anyInt())).thenReturn(true);

        CreateMentionRequest request = new CreateMentionRequest();
        request.setDocumentId(documentId);
        request.setSentenceIndex(0);
        request.setStartTokenIndex(0);
        request.setEndTokenIndex(2);

        assertThrows(ValidationException.class, () -> mentionService.createMention(workspaceId, request, callerId));
    }

    @Test
    @DisplayName("Should get a full (last) page of mentions by workspace")
    void getMentionsByWorkspace() {
        MentionEntity mention1 = createMentionEntity(0, 0, 1);
        MentionEntity mention2 = createMentionEntity(1, 0, 2);

        when(mentionRepository.findPageByWorkspaceId(eq(workspaceId), isNull(), any(Pageable.class)))
                .thenReturn(Arrays.asList(mention1, mention2));

        CursorPage<MentionDto> result = mentionService.getMentionsByWorkspace(workspaceId, callerId, null, 100);

        assertEquals(2, result.items().size());
        assertFalse(result.hasMore());
        assertNull(result.nextCursor());
    }

    @Test
    @DisplayName("Should report hasMore and emit nextCursor when a page overflows")
    void getMentionsByWorkspaceHasMore() {
        // limit=1 -> repo is queried for limit+1=2 rows; both returned means more remain.
        MentionEntity mention1 = createMentionEntity(0, 0, 1);
        UUID firstId = UUID.randomUUID();
        mention1.setId(firstId);
        MentionEntity mention2 = createMentionEntity(1, 0, 2);
        mention2.setId(UUID.randomUUID());

        when(mentionRepository.findPageByWorkspaceId(eq(workspaceId), isNull(), any(Pageable.class)))
                .thenReturn(Arrays.asList(mention1, mention2));

        CursorPage<MentionDto> result = mentionService.getMentionsByWorkspace(workspaceId, callerId, null, 1);

        assertEquals(1, result.items().size());
        assertTrue(result.hasMore());
        assertEquals(firstId.toString(), result.nextCursor());
    }

    @Test
    @DisplayName("Should get mentions by document")
    void getMentionsByDocument() {
        MentionEntity mention = createMentionEntity(0, 0, 1);

        when(mentionRepository.findByDocumentIdOrdered(documentId))
                .thenReturn(Arrays.asList(mention));

        List<MentionDto> result = mentionService.getMentionsByDocument(workspaceId, documentId, callerId);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should assign mention to cluster")
    void assignToCluster() {
        MentionEntity mention = createMentionEntity(0, 0, 1);
        mention.setId(mentionId);

        ClusterEntity cluster = new ClusterEntity();
        cluster.setId(clusterId);
        cluster.setClusterNumber(1);

        when(mentionRepository.findById(mentionId)).thenReturn(Optional.of(mention));
        when(clusterRepository.existsById(clusterId)).thenReturn(true);
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(mentionRepository.save(any(MentionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mentionRepository.countByClusterId(clusterId)).thenReturn(1L);

        MentionDto result = mentionService.assignToCluster(mentionId, clusterId, callerId);

        assertEquals(clusterId, result.getClusterId());
    }

    @Test
    @DisplayName("Should throw when assigning to non-existent cluster")
    void throwOnInvalidCluster() {
        MentionEntity mention = createMentionEntity(0, 0, 1);
        mention.setId(mentionId);

        when(mentionRepository.findById(mentionId)).thenReturn(Optional.of(mention));
        when(clusterRepository.existsById(clusterId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> mentionService.assignToCluster(mentionId, clusterId, callerId));
    }

    @Test
    @DisplayName("Should unassign mention from cluster")
    void unassignFromCluster() {
        MentionEntity mention = createMentionEntity(0, 0, 1);
        mention.setId(mentionId);
        mention.setClusterId(clusterId);

        ClusterEntity cluster = new ClusterEntity();
        cluster.setId(clusterId);
        cluster.setMentionCount(1);

        when(mentionRepository.findById(mentionId)).thenReturn(Optional.of(mention));
        when(mentionRepository.save(any(MentionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(mentionRepository.countByClusterId(clusterId)).thenReturn(0L);

        MentionDto result = mentionService.unassignFromCluster(mentionId, callerId);

        assertNull(result.getClusterId());
    }

    @Test
    @DisplayName("Should delete mention")
    void deleteMention() {
        MentionEntity mention = createMentionEntity(0, 0, 1);
        mention.setId(mentionId);
        mention.setClusterId(clusterId);

        ClusterEntity cluster = new ClusterEntity();
        cluster.setId(clusterId);

        when(mentionRepository.findById(mentionId)).thenReturn(Optional.of(mention));
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(mentionRepository.countByClusterId(clusterId)).thenReturn(0L);

        mentionService.deleteMention(mentionId, callerId);

        verify(mentionRepository).delete(mention);
    }

    @Test
    @DisplayName("Should get unassigned mentions")
    void getUnassignedMentions() {
        MentionEntity mention = createMentionEntity(0, 0, 1);
        mention.setClusterId(null);

        when(mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId))
                .thenReturn(Arrays.asList(mention));

        List<MentionDto> result = mentionService.getUnassignedMentions(workspaceId, callerId);

        assertEquals(1, result.size());
        assertNull(result.get(0).getClusterId());
    }

    @Test
    @DisplayName("Should include cluster info in DTO")
    void includeClusterInfo() {
        MentionEntity mention = createMentionEntity(0, 0, 1);
        mention.setId(mentionId);
        mention.setClusterId(clusterId);

        ClusterEntity cluster = new ClusterEntity();
        cluster.setId(clusterId);
        cluster.setClusterNumber(1);
        cluster.setColor("#FF0000");

        when(mentionRepository.findById(mentionId)).thenReturn(Optional.of(mention));
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));

        MentionDto result = mentionService.getMention(mentionId, callerId);

        assertEquals(1, result.getClusterNumber());
        assertEquals("#FF0000", result.getClusterColor());
    }

    private MentionEntity createMentionEntity(int sentenceIdx, int startToken, int endToken) {
        MentionEntity entity = new MentionEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setDocumentId(documentId);
        entity.setSentenceIndex(sentenceIdx);
        entity.setStartTokenIndex(startToken);
        entity.setEndTokenIndex(endToken);
        entity.setText("Test");
        return entity;
    }
}
