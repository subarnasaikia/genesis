package com.genesis.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.genesis.editor.dto.DocumentContentResponse;
import com.genesis.editor.dto.EditorDocumentInfo;
import com.genesis.editor.dto.EditorSessionResponse;
import com.genesis.editor.dto.WorkspaceEditorResponse;
import com.genesis.editor.entity.EditorSession;
import com.genesis.editor.repository.EditorSessionRepository;
import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.service.ImportService;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.dto.WorkspaceResponse;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.service.DocumentService;
import com.genesis.workspace.service.WorkspaceService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EditorService.
 */
@ExtendWith(MockitoExtension.class)
class EditorServiceTest {

    @Mock
    private EditorSessionRepository sessionRepository;

    @Mock
    private ImportService importService;

    @Mock
    private DocumentService documentService;

    @Mock
    private WorkspaceService workspaceService;

    private EditorService editorService;

    private UUID workspaceId;
    private UUID userId;
    private UUID documentId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        editorService = new EditorService(sessionRepository, importService, documentService, workspaceService);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should open workspace and create session")
    void openWorkspaceCreatesSession() {
        when(sessionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(EditorSession.class))).thenAnswer(inv -> {
            EditorSession session = inv.getArgument(0);
            session.setId(sessionId);
            return session;
        });
        when(workspaceService.getById(workspaceId, userId)).thenReturn(createWorkspaceResponse());
        when(documentService.getByWorkspaceIdInternal(workspaceId)).thenReturn(Arrays.asList(createDocumentResponse()));
        when(importService.isTokenized(documentId)).thenReturn(true);
        when(importService.getSentenceCount(documentId)).thenReturn(5L);
        when(importService.getTokenCount(documentId)).thenReturn(50L);

        WorkspaceEditorResponse result = editorService.openWorkspace(workspaceId, userId);

        assertNotNull(result);
        assertEquals(workspaceId, result.getWorkspaceId());
        assertEquals(1, result.getTotalDocuments());
        assertEquals(5, result.getTotalSentences());
        assertEquals(50, result.getTotalTokens());
        assertNotNull(result.getSession());
    }

    @Test
    @DisplayName("Should open workspace with existing session")
    void openWorkspaceExistingSession() {
        EditorSession existingSession = createSession();
        existingSession.setLastDocumentIndex(2);
        existingSession.setScrollPosition(100);

        when(sessionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(existingSession));
        when(sessionRepository.save(any(EditorSession.class))).thenReturn(existingSession);
        when(workspaceService.getById(workspaceId, userId)).thenReturn(createWorkspaceResponse());
        when(documentService.getByWorkspaceIdInternal(workspaceId)).thenReturn(Arrays.asList());

        WorkspaceEditorResponse result = editorService.openWorkspace(workspaceId, userId);

        assertEquals(2, result.getSession().getLastDocumentIndex());
        assertEquals(100, result.getSession().getScrollPosition());
    }

    @Test
    @DisplayName("Should get document content with tokens (default first page)")
    void getDocumentContent() {
        when(documentService.getByIdInternal(documentId)).thenReturn(createDocumentResponse());
        when(importService.getSentenceCount(documentId)).thenReturn(1L);
        when(importService.getTokenCount(documentId)).thenReturn(1L);
        when(importService.getSentencesPage(documentId, 0, 50)).thenReturn(Arrays.asList(createSentence()));
        when(importService.getTokensInSentenceRange(documentId, 0, 0)).thenReturn(Arrays.asList(createToken()));

        DocumentContentResponse result = editorService.getDocumentContent(documentId);

        assertNotNull(result);
        assertEquals(documentId, result.getDocumentId());
        assertEquals(1, result.getSentences().size());
        assertEquals(1, result.getTokens().size());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(50, result.getPageSize());
        assertEquals(1, result.getTotalSentences());
    }

    @Test
    @DisplayName("Should paginate document content by sentences")
    void getDocumentContentPaginated() {
        when(documentService.getByIdInternal(documentId)).thenReturn(createDocumentResponse());
        when(importService.getSentenceCount(documentId)).thenReturn(120L);
        when(importService.getTokenCount(documentId)).thenReturn(1200L);
        SentenceEntity s50 = createSentence();
        s50.setSentenceIndex(50);
        SentenceEntity s99 = createSentence();
        s99.setSentenceIndex(99);
        when(importService.getSentencesPage(documentId, 1, 50)).thenReturn(Arrays.asList(s50, s99));
        when(importService.getTokensInSentenceRange(documentId, 50, 99)).thenReturn(Arrays.asList(createToken()));

        DocumentContentResponse result = editorService.getDocumentContent(documentId, 1, 50);

        assertEquals(1, result.getCurrentPage());
        assertEquals(3, result.getTotalPages()); // ceil(120/50) = 3
        assertEquals(50, result.getPageSize());
        assertEquals(120, result.getTotalSentences());
        assertEquals(2, result.getSentences().size());
    }

    @Test
    @DisplayName("Should return empty page when no sentences in range")
    void getDocumentContentEmptyPage() {
        when(documentService.getByIdInternal(documentId)).thenReturn(createDocumentResponse());
        when(importService.getSentenceCount(documentId)).thenReturn(0L);
        when(importService.getTokenCount(documentId)).thenReturn(0L);
        when(importService.getSentencesPage(documentId, 0, 50)).thenReturn(Arrays.asList());

        DocumentContentResponse result = editorService.getDocumentContent(documentId, 0, 50);

        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getSentences().size());
        assertEquals(0, result.getTokens().size());
    }

    @Test
    @DisplayName("Should calculate global token offset")
    void getDocumentContentWithOffset() {
        UUID doc1 = UUID.randomUUID();
        UUID doc2 = documentId;

        DocumentResponse docResp1 = createDocumentResponse();
        docResp1.setId(doc1);
        docResp1.setOrderIndex(0);

        DocumentResponse docResp2 = createDocumentResponse();
        docResp2.setId(doc2);
        docResp2.setOrderIndex(1);

        when(documentService.getByWorkspaceIdInternal(workspaceId))
                .thenReturn(Arrays.asList(docResp1, docResp2));
        when(documentService.getByIdInternal(doc2)).thenReturn(docResp2);
        when(importService.getSentenceCount(doc2)).thenReturn(0L);
        when(importService.getTokenCount(doc2)).thenReturn(0L);
        when(importService.getSentencesPage(doc2, 0, 50)).thenReturn(Arrays.asList());
        when(importService.getTokenCount(doc1)).thenReturn(100L);

        DocumentContentResponse result = editorService.getDocumentContentWithOffset(workspaceId, doc2);

        assertEquals(100, result.getGlobalTokenOffset());
    }

    @Test
    @DisplayName("Should get workspace documents with token counts")
    void getWorkspaceDocuments() {
        DocumentResponse doc = createDocumentResponse();
        when(documentService.getByWorkspaceIdInternal(workspaceId)).thenReturn(Arrays.asList(doc));
        when(importService.isTokenized(documentId)).thenReturn(true);
        when(importService.getSentenceCount(documentId)).thenReturn(10L);
        when(importService.getTokenCount(documentId)).thenReturn(100L);

        List<EditorDocumentInfo> result = editorService.getWorkspaceDocuments(workspaceId);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getSentenceCount());
        assertEquals(100, result.get(0).getTokenCount());
        assertTrue(result.get(0).getIsTokenized());
    }

    @Test
    @DisplayName("Should save session state")
    void saveSession() {
        EditorSession existing = createSession();
        when(sessionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(existing));
        when(sessionRepository.save(any(EditorSession.class))).thenAnswer(inv -> inv.getArgument(0));

        EditorSessionResponse result = editorService.saveSession(workspaceId, userId, 5, 200);

        assertEquals(5, result.getLastDocumentIndex());
        assertEquals(200, result.getScrollPosition());
    }

    @Test
    @DisplayName("Should close session")
    void closeSession() {
        editorService.closeSession(workspaceId, userId);

        verify(sessionRepository).deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }

    private EditorSession createSession() {
        EditorSession session = new EditorSession();
        session.setId(sessionId);
        session.setWorkspaceId(workspaceId);
        session.setUserId(userId);
        session.setLastDocumentIndex(0);
        session.setScrollPosition(0);
        session.setLastAccessedAt(Instant.now());
        return session;
    }

    private WorkspaceResponse createWorkspaceResponse() {
        WorkspaceResponse response = new WorkspaceResponse();
        response.setId(workspaceId);
        response.setName("Test Workspace");
        return response;
    }

    private DocumentResponse createDocumentResponse() {
        DocumentResponse response = new DocumentResponse();
        response.setId(documentId);
        response.setName("test.txt");
        response.setOrderIndex(0);
        response.setStatus(DocumentStatus.UPLOADED);
        return response;
    }

    private SentenceEntity createSentence() {
        SentenceEntity sentence = new SentenceEntity();
        sentence.setId(UUID.randomUUID());
        sentence.setDocumentId(documentId);
        sentence.setSentenceIndex(0);
        sentence.setText("Test sentence.");
        sentence.setTokenCount(2);
        return sentence;
    }

    private TokenEntity createToken() {
        TokenEntity token = new TokenEntity();
        token.setId(UUID.randomUUID());
        token.setDocumentId(documentId);
        token.setSentenceIndex(0);
        token.setTokenIndex(0);
        token.setForm("Test");
        return token;
    }
}
