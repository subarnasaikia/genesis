package com.genesis.wsd.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.common.port.DocumentQueryPort;
import com.genesis.common.port.TokenQueryPort;
import com.genesis.workspace.service.WorkspaceAccessControl;
import com.genesis.wsd.dto.CreateWsdAnnotationRequest;
import com.genesis.wsd.dto.WsdAnnotationDto;
import com.genesis.wsd.entity.WsdAnnotationEntity;
import com.genesis.wsd.entity.WsdSenseEntity;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import com.genesis.wsd.repository.WsdSenseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WsdAnnotationServiceTest {

    @Mock
    private WsdAnnotationRepository annotationRepository;
    @Mock
    private WsdSenseRepository senseRepository;
    @Mock
    private TokenQueryPort tokenQuery;
    @Mock
    private DocumentQueryPort documentQuery;
    @Mock
    private WorkspaceAccessControl accessControl;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private WsdAnnotationService service;

    private UUID workspaceId;
    private UUID userId;
    private UUID tokenId;
    private UUID senseId;
    private static final String ANNOTATOR = "alice";

    @BeforeEach
    void setUp() {
        service = new WsdAnnotationService(annotationRepository, senseRepository,
                tokenQuery, documentQuery, accessControl, eventPublisher);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tokenId = UUID.randomUUID();
        senseId = UUID.randomUUID();
    }

    private WsdSenseEntity senseIn(UUID wsId) {
        WsdSenseEntity s = new WsdSenseEntity();
        s.setId(senseId);
        s.setWorkspaceId(wsId);
        return s;
    }

    @Test
    @DisplayName("Non-member GET → UnauthorizedException(forbidden=true) — 403 (eng-review D5)")
    void getByToken_nonMember_returnsForbidden() {
        doThrow(new UnauthorizedException("Not a member of this workspace", true))
                .when(accessControl).requireMember(workspaceId, userId);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.getByToken(workspaceId, tokenId, userId));
        assertTrue(ex.isForbidden(), "must map to 403, not 401");
    }

    @Test
    @DisplayName("Cross-workspace tokenId → ValidationException")
    void upsert_crossWorkspaceToken_rejected() {
        UUID docId = UUID.randomUUID();
        UUID otherWorkspace = UUID.randomUUID();
        when(tokenQuery.documentIdForToken(tokenId)).thenReturn(docId);
        when(documentQuery.workspaceIdForDocument(docId)).thenReturn(otherWorkspace);

        CreateWsdAnnotationRequest req = new CreateWsdAnnotationRequest();
        req.setTokenId(tokenId);
        req.setSenseId(senseId);

        assertThrows(ValidationException.class,
                () -> service.upsert(workspaceId, userId, ANNOTATOR, req));

        verify(annotationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByDocument: enriches each annotation with its sense label")
    void getByDocument_enrichesSenseLabel() {
        UUID documentId = UUID.randomUUID();
        UUID senseB = UUID.randomUUID();

        WsdAnnotationEntity a1 = new WsdAnnotationEntity();
        a1.setTokenId(tokenId);
        a1.setSenseId(senseId);
        a1.setAnnotatorId(ANNOTATOR);
        a1.setWorkspaceId(workspaceId);

        WsdAnnotationEntity a2 = new WsdAnnotationEntity();
        a2.setTokenId(UUID.randomUUID());
        a2.setSenseId(senseB);
        a2.setAnnotatorId(ANNOTATOR);
        a2.setWorkspaceId(workspaceId);

        WsdSenseEntity s1 = new WsdSenseEntity();
        s1.setId(senseId);
        s1.setWorkspaceId(workspaceId);
        s1.setSenseLabel("bank#1");
        WsdSenseEntity s2 = new WsdSenseEntity();
        s2.setId(senseB);
        s2.setWorkspaceId(workspaceId);
        s2.setSenseLabel("bank#2");

        when(annotationRepository.findByWorkspaceIdAndDocumentId(workspaceId, documentId))
                .thenReturn(List.of(a1, a2));
        when(senseRepository.findAllById(any())).thenReturn(List.of(s1, s2));

        List<WsdAnnotationDto> result = service.getByDocument(workspaceId, documentId, userId);

        assertEquals(2, result.size());
        assertEquals("bank#1",
                result.stream().filter(d -> d.getSenseId().equals(senseId)).findFirst().orElseThrow().getSenseLabel());
        assertEquals("bank#2",
                result.stream().filter(d -> d.getSenseId().equals(senseB)).findFirst().orElseThrow().getSenseLabel());
        verify(accessControl).requireMember(workspaceId, userId);
    }

    @Test
    @DisplayName("getByDocument: non-member → UnauthorizedException(forbidden=true)")
    void getByDocument_nonMember_returnsForbidden() {
        UUID documentId = UUID.randomUUID();
        doThrow(new UnauthorizedException("Not a member of this workspace", true))
                .when(accessControl).requireMember(workspaceId, userId);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.getByDocument(workspaceId, documentId, userId));
        assertTrue(ex.isForbidden());
    }

    @Test
    @DisplayName("Upsert: same annotator re-tags → reuses existing row, no duplicate")
    void upsert_sameAnnotator_reusesExisting() {
        UUID docId = UUID.randomUUID();
        when(tokenQuery.documentIdForToken(tokenId)).thenReturn(docId);
        when(documentQuery.workspaceIdForDocument(docId)).thenReturn(workspaceId);
        when(senseRepository.findById(senseId)).thenReturn(Optional.of(senseIn(workspaceId)));

        WsdAnnotationEntity existing = new WsdAnnotationEntity();
        existing.setTokenId(tokenId);
        existing.setAnnotatorId(ANNOTATOR);
        existing.setWorkspaceId(workspaceId);
        existing.setSenseId(UUID.randomUUID());
        when(annotationRepository.findByTokenIdAndAnnotatorId(tokenId, ANNOTATOR))
                .thenReturn(Optional.of(existing));
        when(annotationRepository.save(any(WsdAnnotationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateWsdAnnotationRequest req = new CreateWsdAnnotationRequest();
        req.setTokenId(tokenId);
        req.setSenseId(senseId);

        WsdAnnotationDto saved = service.upsert(workspaceId, userId, ANNOTATOR, req);

        assertEquals(senseId, saved.getSenseId());
        assertEquals(ANNOTATOR, saved.getAnnotatorId());
        verify(annotationRepository, times(1)).save(existing);
    }
}
