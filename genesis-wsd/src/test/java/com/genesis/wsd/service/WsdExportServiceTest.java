package com.genesis.wsd.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.genesis.workspace.service.WorkspaceAccessControl;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WsdExportServiceTest {

    @Mock
    private WsdAnnotationRepository annotationRepository;
    @Mock
    private WorkspaceAccessControl accessControl;

    private WsdExportService service;

    private UUID workspaceId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new WsdExportService(annotationRepository, accessControl);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Per-annotator TSV: row count = total annotations (+ header)")
    void perAnnotator_rowCountMatchesTotal() {
        UUID tok1 = UUID.randomUUID();
        UUID tok2 = UUID.randomUUID();
        when(annotationRepository.findPerAnnotatorExportRows(workspaceId)).thenReturn(List.of(
                new Object[]{tok1, "bank", "financial", "alice"},
                new Object[]{tok1, "bank", "river", "bob"},
                new Object[]{tok2, "bat", "animal", "alice"}));

        String tsv = service.exportPerAnnotator(workspaceId, userId);

        String[] lines = tsv.split("\n");
        assertEquals(4, lines.length, "1 header + 3 rows = 4 lines");
        assertEquals("token_id\tword\tsense_label\tannotator_id", lines[0]);
        assertTrue(lines[1].contains("alice"));
        assertTrue(lines[3].contains("bat"));
    }

    @Test
    @DisplayName("Consensus picks most common sense per token")
    void consensus_picksMostCommonSense() {
        UUID tokenId = UUID.randomUUID();
        // Repository ORDER BY puts the winner first per tokenId.
        // (financial: 2 votes) wins over (river: 1 vote).
        when(annotationRepository.findConsensusExportRows(workspaceId)).thenReturn(List.of(
                new Object[]{tokenId, "bank", "financial", 2L, Instant.now()},
                new Object[]{tokenId, "bank", "river", 1L, Instant.now()}));

        String tsv = service.exportConsensus(workspaceId, userId);

        String[] lines = tsv.split("\n");
        assertEquals(2, lines.length, "1 header + 1 winner row");
        assertEquals("token_id\tword\tsense_label\tvotes", lines[0]);
        assertTrue(lines[1].endsWith("\tfinancial\t2"),
                "winner row must end with sense + votes: " + lines[1]);
    }

    @Test
    @DisplayName("Consensus tie broken by most recent timestamp")
    void consensus_tieBrokenByMostRecent() {
        UUID tokenId = UUID.randomUUID();
        Instant later = Instant.now();
        Instant earlier = later.minusSeconds(60);
        // Both 1 vote; repo orders by MAX(timestamp) DESC, so later wins.
        when(annotationRepository.findConsensusExportRows(workspaceId)).thenReturn(List.of(
                new Object[]{tokenId, "bank", "river", 1L, later},
                new Object[]{tokenId, "bank", "financial", 1L, earlier}));

        String tsv = service.exportConsensus(workspaceId, userId);

        String[] lines = tsv.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].contains("river"),
                "winner must be the most recent on tie: " + lines[1]);
    }
}
