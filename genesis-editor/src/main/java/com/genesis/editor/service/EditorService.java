package com.genesis.editor.service;

import com.genesis.editor.dto.DocumentContentResponse;
import com.genesis.editor.dto.EditorDocumentInfo;
import com.genesis.editor.dto.EditorSessionResponse;
import com.genesis.editor.dto.WorkspaceEditorResponse;
import com.genesis.editor.entity.EditorSession;
import com.genesis.editor.repository.EditorSessionRepository;
import com.genesis.importexport.dto.SentenceDto;
import com.genesis.importexport.dto.TokenDto;
import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.service.ImportService;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.service.DocumentService;
import com.genesis.workspace.service.WorkspaceService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing editor sessions and serving document content.
 */
@Service
public class EditorService {

    private final EditorSessionRepository editorSessionRepository;
    private final ImportService importService;
    private final DocumentService documentService;
    private final WorkspaceService workspaceService;

    public EditorService(EditorSessionRepository editorSessionRepository,
            ImportService importService,
            DocumentService documentService,
            WorkspaceService workspaceService) {
        this.editorSessionRepository = editorSessionRepository;
        this.importService = importService;
        this.documentService = documentService;
        this.workspaceService = workspaceService;
    }

    /**
     * Open a workspace in the editor.
     * Returns session info, documents list, and aggregate stats.
     */
    @Transactional
    public WorkspaceEditorResponse openWorkspace(@NonNull UUID workspaceId, @NonNull UUID userId) {
        // Get or create session
        EditorSession session = editorSessionRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseGet(() -> {
                    EditorSession newSession = new EditorSession();
                    newSession.setWorkspaceId(workspaceId);
                    newSession.setUserId(userId);
                    newSession.setLastDocumentIndex(0);
                    newSession.setScrollPosition(0);
                    newSession.setLastAccessedAt(Instant.now());
                    return newSession;
                });

        session.setLastAccessedAt(Instant.now());
        EditorSession savedSession = editorSessionRepository.save(session);

        // Get workspace info
        var workspaceInfo = workspaceService.getById(workspaceId);

        // Get all documents
        List<DocumentResponse> documents = documentService.getByWorkspaceId(workspaceId);

        // Build document info with token counts
        List<EditorDocumentInfo> documentInfos = new ArrayList<>();
        int totalSentences = 0;
        int totalTokens = 0;
        int tokenizedDocuments = 0;

        for (DocumentResponse doc : documents) {
            EditorDocumentInfo info = new EditorDocumentInfo();
            info.setId(doc.getId());
            info.setName(doc.getName());
            info.setOrderIndex(doc.getOrderIndex());
            info.setStatus(doc.getStatus() != null ? doc.getStatus().name() : "UNKNOWN");

            // Check if tokenized
            boolean isTokenized = importService.isTokenized(doc.getId());
            info.setIsTokenized(isTokenized);

            if (isTokenized) {
                int sentenceCount = (int) importService.getSentenceCount(doc.getId());
                int tokenCount = (int) importService.getTokenCount(doc.getId());
                info.setSentenceCount(sentenceCount);
                info.setTokenCount(tokenCount);
                totalSentences += sentenceCount;
                totalTokens += tokenCount;
                tokenizedDocuments++;
            } else {
                info.setSentenceCount(0);
                info.setTokenCount(0);
            }

            documentInfos.add(info);
        }

        // Build response
        WorkspaceEditorResponse response = new WorkspaceEditorResponse();
        response.setWorkspaceId(workspaceId);
        response.setWorkspaceName(workspaceInfo.getName());
        response.setSession(mapToSessionResponse(savedSession));
        response.setDocuments(documentInfos);
        response.setTotalDocuments(documents.size());
        response.setTotalSentences(totalSentences);
        response.setTotalTokens(totalTokens);
        response.setTokenizedDocuments(tokenizedDocuments);
        response.setLastAccessedAt(savedSession.getLastAccessedAt());

        return response;
    }

    /**
     * Get document content with tokens for display.
     */
    public DocumentContentResponse getDocumentContent(@NonNull UUID documentId) {
        DocumentResponse docInfo = documentService.getById(documentId);
        List<SentenceEntity> sentences = importService.getSentences(documentId);
        List<TokenEntity> tokens = importService.getTokens(documentId);

        DocumentContentResponse response = new DocumentContentResponse();
        response.setDocumentId(documentId);
        response.setDocumentName(docInfo.getName());
        response.setOrderIndex(docInfo.getOrderIndex());
        response.setSentences(sentences.stream().map(this::mapToSentenceDto).collect(Collectors.toList()));
        response.setTokens(tokens.stream().map(this::mapToTokenDto).collect(Collectors.toList()));
        response.setTotalSentences(sentences.size());
        response.setTotalTokens(tokens.size());
        response.setGlobalTokenOffset(0); // Will be calculated per-workspace if needed

        return response;
    }

    /**
     * Get document content with workspace-level token offset.
     */
    public DocumentContentResponse getDocumentContentWithOffset(@NonNull UUID workspaceId,
            @NonNull UUID documentId) {
        DocumentContentResponse response = getDocumentContent(documentId);

        // Calculate global offset from previous documents
        List<DocumentResponse> allDocs = documentService.getByWorkspaceId(workspaceId);
        int globalOffset = 0;

        for (DocumentResponse doc : allDocs) {
            if (doc.getId().equals(documentId)) {
                break;
            }
            globalOffset += (int) importService.getTokenCount(doc.getId());
        }

        response.setGlobalTokenOffset(globalOffset);
        return response;
    }

    /**
     * Get all documents info for a workspace.
     */
    public List<EditorDocumentInfo> getWorkspaceDocuments(@NonNull UUID workspaceId) {
        List<DocumentResponse> documents = documentService.getByWorkspaceId(workspaceId);
        List<EditorDocumentInfo> result = new ArrayList<>();

        for (DocumentResponse doc : documents) {
            EditorDocumentInfo info = new EditorDocumentInfo();
            info.setId(doc.getId());
            info.setName(doc.getName());
            info.setOrderIndex(doc.getOrderIndex());
            info.setStatus(doc.getStatus() != null ? doc.getStatus().name() : "UNKNOWN");

            boolean isTokenized = importService.isTokenized(doc.getId());
            info.setIsTokenized(isTokenized);

            if (isTokenized) {
                info.setSentenceCount((int) importService.getSentenceCount(doc.getId()));
                info.setTokenCount((int) importService.getTokenCount(doc.getId()));
            } else {
                info.setSentenceCount(0);
                info.setTokenCount(0);
            }

            result.add(info);
        }

        return result;
    }

    /**
     * Get the current session for a user and workspace.
     */
    public Optional<EditorSessionResponse> getSession(@NonNull UUID workspaceId, @NonNull UUID userId) {
        return editorSessionRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(this::mapToSessionResponse);
    }

    /**
     * Save session state.
     */
    @Transactional
    public EditorSessionResponse saveSession(@NonNull UUID workspaceId, @NonNull UUID userId,
            Integer documentIndex, Integer scrollPosition) {
        EditorSession session = editorSessionRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseGet(() -> {
                    EditorSession newSession = new EditorSession();
                    newSession.setWorkspaceId(workspaceId);
                    newSession.setUserId(userId);
                    return newSession;
                });

        if (documentIndex != null) {
            session.setLastDocumentIndex(documentIndex);
        }
        if (scrollPosition != null) {
            session.setScrollPosition(scrollPosition);
        }
        session.setLastAccessedAt(Instant.now());

        EditorSession saved = editorSessionRepository.save(session);
        return mapToSessionResponse(saved);
    }

    /**
     * Close/clear the session for a workspace.
     */
    @Transactional
    public void closeSession(@NonNull UUID workspaceId, @NonNull UUID userId) {
        editorSessionRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }

    private EditorSessionResponse mapToSessionResponse(EditorSession session) {
        EditorSessionResponse response = new EditorSessionResponse();
        response.setId(session.getId());
        response.setWorkspaceId(session.getWorkspaceId());
        response.setUserId(session.getUserId());
        response.setLastDocumentIndex(session.getLastDocumentIndex());
        response.setScrollPosition(session.getScrollPosition());
        response.setLastAccessedAt(session.getLastAccessedAt());
        return response;
    }

    private SentenceDto mapToSentenceDto(SentenceEntity entity) {
        SentenceDto dto = new SentenceDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setSentenceIndex(entity.getSentenceIndex());
        dto.setText(entity.getText());
        dto.setStartOffset(entity.getStartOffset());
        dto.setEndOffset(entity.getEndOffset());
        dto.setTokenCount(entity.getTokenCount());
        return dto;
    }

    private TokenDto mapToTokenDto(TokenEntity entity) {
        TokenDto dto = new TokenDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setSentenceIndex(entity.getSentenceIndex());
        dto.setTokenIndex(entity.getTokenIndex());
        dto.setGlobalIndex(entity.getGlobalIndex());
        dto.setForm(entity.getForm());
        dto.setPos(entity.getPos());
        dto.setLemma(entity.getLemma());
        dto.setNerTag(entity.getNerTag());
        dto.setStartOffset(entity.getStartOffset());
        dto.setEndOffset(entity.getEndOffset());
        return dto;
    }
}
