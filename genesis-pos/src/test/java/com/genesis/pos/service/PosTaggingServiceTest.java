package com.genesis.pos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.ValidationException;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.repository.TokenRepository;
import com.genesis.pos.dto.PosAnnotationDto;
import com.genesis.pos.entity.PosAnnotationEntity;
import com.genesis.pos.repository.PosAnnotationRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PosTaggingServiceTest {

    @Mock
    private PosAnnotationRepository posRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PosTagDefinitionService tagDefinitionService;

    @Mock
    private com.genesis.workspace.service.WorkspaceAccessControl accessControl;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PosTaggingService service;

    private UUID tokenId;
    private UUID documentId;
    private UUID workspaceId;
    private UUID callerId;
    private static final String ANNOTATOR = "alice";

    @BeforeEach
    void setUp() {
        service = new PosTaggingService(
                posRepository, tokenRepository, documentRepository, tagDefinitionService, accessControl, eventPublisher);
        tokenId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        callerId = UUID.randomUUID();
    }

    private TokenEntity token() {
        TokenEntity t = new TokenEntity();
        t.setDocumentId(documentId);
        return t;
    }

    private Document document() {
        Workspace ws = new Workspace();
        ws.setId(workspaceId);
        Document d = new Document();
        d.setWorkspace(ws);
        return d;
    }

    private void stubValidLookups() {
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token()));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document()));
    }

    @Test
    @DisplayName("updatePos with a valid Universal POS tag persists a new row")
    void updatePos_validUdTag_persists() {
        stubValidLookups();
        when(tagDefinitionService.effectiveTagSet(workspaceId, callerId))
                .thenReturn(Set.of("NOUN", "VERB"));
        when(posRepository.findByTokenIdAndAnnotatorId(tokenId, ANNOTATOR)).thenReturn(Optional.empty());
        when(posRepository.save(any(PosAnnotationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PosAnnotationDto dto = service.updatePos(tokenId, callerId, ANNOTATOR, "NOUN");

        assertNotNull(dto);
        assertEquals("NOUN", dto.getPosTag());
        assertEquals(ANNOTATOR, dto.getAnnotatorId());
        assertEquals(tokenId, dto.getTokenId());

        ArgumentCaptor<PosAnnotationEntity> captor = ArgumentCaptor.forClass(PosAnnotationEntity.class);
        verify(posRepository).save(captor.capture());
        assertEquals(documentId, captor.getValue().getDocumentId());
    }

    @Test
    @DisplayName("updatePos accepts a custom workspace tag from the effective set")
    void updatePos_customWorkspaceTag_persists() {
        stubValidLookups();
        when(tagDefinitionService.effectiveTagSet(workspaceId, callerId))
                .thenReturn(Set.of("NOUN", "NEG"));
        when(posRepository.findByTokenIdAndAnnotatorId(tokenId, ANNOTATOR)).thenReturn(Optional.empty());
        when(posRepository.save(any(PosAnnotationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PosAnnotationDto dto = service.updatePos(tokenId, callerId, ANNOTATOR, "NEG");

        assertEquals("NEG", dto.getPosTag());
        verify(posRepository).save(any());
    }

    @Test
    @DisplayName("updatePos with a tag outside the effective set throws ValidationException")
    void updatePos_invalidTag_throwsValidationException() {
        stubValidLookups();
        when(tagDefinitionService.effectiveTagSet(workspaceId, callerId))
                .thenReturn(Set.of("NOUN", "VERB"));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.updatePos(tokenId, callerId, ANNOTATOR, "BOGUS"));
        assertTrue(ex.getMessage().contains("BOGUS"));
        verify(posRepository, never()).save(any());
    }

    @Test
    @DisplayName("Second update from same annotator upserts existing row, no duplicate")
    void updatePos_secondAnnotatorOnSameToken_upsertsExistingRowNoDuplicate() {
        PosAnnotationEntity existing = new PosAnnotationEntity();
        existing.setTokenId(tokenId);
        existing.setDocumentId(documentId);
        existing.setAnnotatorId(ANNOTATOR);
        existing.setPosTag("NOUN");

        stubValidLookups();
        when(tagDefinitionService.effectiveTagSet(workspaceId, callerId))
                .thenReturn(Set.of("NOUN", "VERB"));
        when(posRepository.findByTokenIdAndAnnotatorId(tokenId, ANNOTATOR)).thenReturn(Optional.of(existing));
        when(posRepository.save(any(PosAnnotationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PosAnnotationDto dto = service.updatePos(tokenId, callerId, ANNOTATOR, "VERB");

        assertEquals("VERB", dto.getPosTag());
        ArgumentCaptor<PosAnnotationEntity> captor = ArgumentCaptor.forClass(PosAnnotationEntity.class);
        verify(posRepository, times(1)).save(captor.capture());
        assertSame(existing, captor.getValue());
        assertEquals("VERB", existing.getPosTag());
    }

    @Test
    @DisplayName("updatePos with null tag deletes annotator's row")
    void updatePos_nullTag_deletesRow() {
        stubValidLookups();
        PosAnnotationDto dto = service.updatePos(tokenId, callerId, ANNOTATOR, null);

        assertNull(dto);
        verify(posRepository).deleteByTokenIdAndAnnotatorId(tokenId, ANNOTATOR);
        verify(posRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePos rejects blank or null annotator")
    void updatePos_blankAnnotator_rejected() {
        assertThrows(ValidationException.class, () -> service.updatePos(tokenId, callerId, "", "NOUN"));
        assertThrows(ValidationException.class, () -> service.updatePos(tokenId, callerId, null, "NOUN"));
    }

    @Test
    @DisplayName("getMajorityPosByDocument: single annotator wins")
    void majority_singleAnnotator_returnsTheirTag() {
        UUID t1 = UUID.randomUUID();
        List<Object[]> rows = Collections.singletonList(
                new Object[] { t1, "NOUN", 1L, Instant.now() });
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document()));
        when(posRepository.findPosCountsByDocumentId(documentId)).thenReturn(rows);

        Map<UUID, String> majority = service.getMajorityPosByDocument(documentId, callerId);

        assertEquals(1, majority.size());
        assertEquals("NOUN", majority.get(t1));
    }

    @Test
    @DisplayName("getMajorityPosByDocument: 2-vs-1 vote, majority wins regardless of timestamp")
    void majority_twoVsOne_majorityWins() {
        UUID t1 = UUID.randomUUID();
        Instant now = Instant.now();
        List<Object[]> rows = Arrays.asList(
                new Object[] { t1, "NOUN", 2L, now.minusSeconds(60) },
                new Object[] { t1, "VERB", 1L, now });
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document()));
        when(posRepository.findPosCountsByDocumentId(documentId)).thenReturn(rows);

        Map<UUID, String> majority = service.getMajorityPosByDocument(documentId, callerId);

        assertEquals("NOUN", majority.get(t1));
    }

    @Test
    @DisplayName("getMajorityPosByDocument: tied votes broken by most recent timestamp")
    void majority_tied_mostRecentWins() {
        UUID t1 = UUID.randomUUID();
        Instant now = Instant.now();
        List<Object[]> rows = Arrays.asList(
                new Object[] { t1, "VERB", 1L, now },
                new Object[] { t1, "NOUN", 1L, now.minusSeconds(60) });
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document()));
        when(posRepository.findPosCountsByDocumentId(documentId)).thenReturn(rows);

        Map<UUID, String> majority = service.getMajorityPosByDocument(documentId, callerId);

        assertEquals("VERB", majority.get(t1));
    }
}
