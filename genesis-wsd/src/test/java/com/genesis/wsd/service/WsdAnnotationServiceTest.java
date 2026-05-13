package com.genesis.wsd.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.repository.TokenRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.wsd.dto.CreateWsdAnnotationRequest;
import com.genesis.wsd.dto.WsdAnnotationDto;
import com.genesis.wsd.entity.WsdAnnotationEntity;
import com.genesis.wsd.entity.WsdSenseEntity;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import com.genesis.wsd.repository.WsdSenseRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WsdAnnotationServiceTest {

    @Mock
    private WsdAnnotationRepository annotationRepository;
    @Mock
    private WsdSenseRepository senseRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private WorkspaceMemberRepository memberRepository;

    private WsdAnnotationService service;

    private UUID workspaceId;
    private UUID userId;
    private UUID tokenId;
    private UUID senseId;
    private static final String ANNOTATOR = "alice";

    @BeforeEach
    void setUp() {
        service = new WsdAnnotationService(annotationRepository, senseRepository,
                tokenRepository, documentRepository, memberRepository);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tokenId = UUID.randomUUID();
        senseId = UUID.randomUUID();
    }

    private void mockMember() {
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
    }

    private TokenEntity tokenInDoc(UUID docId) {
        TokenEntity t = new TokenEntity();
        t.setDocumentId(docId);
        return t;
    }

    private Document docInWorkspace(UUID wsId) {
        Document d = new Document();
        Workspace w = new Workspace();
        w.setId(wsId);
        d.setWorkspace(w);
        return d;
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
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.getByToken(workspaceId, tokenId, userId));
        assertTrue(ex.isForbidden(), "must map to 403, not 401");
    }

    @Test
    @DisplayName("Cross-workspace tokenId → ValidationException")
    void upsert_crossWorkspaceToken_rejected() {
        mockMember();
        UUID docId = UUID.randomUUID();
        UUID otherWorkspace = UUID.randomUUID();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(tokenInDoc(docId)));
        when(documentRepository.findById(docId)).thenReturn(Optional.of(docInWorkspace(otherWorkspace)));

        CreateWsdAnnotationRequest req = new CreateWsdAnnotationRequest();
        req.setTokenId(tokenId);
        req.setSenseId(senseId);

        assertThrows(ValidationException.class,
                () -> service.upsert(workspaceId, userId, ANNOTATOR, req));

        verify(annotationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Upsert: same annotator re-tags → reuses existing row, no duplicate")
    void upsert_sameAnnotator_reusesExisting() {
        mockMember();
        UUID docId = UUID.randomUUID();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(tokenInDoc(docId)));
        when(documentRepository.findById(docId)).thenReturn(Optional.of(docInWorkspace(workspaceId)));
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
