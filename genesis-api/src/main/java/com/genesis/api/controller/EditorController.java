package com.genesis.api.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.editor.dto.DocumentContentResponse;
import com.genesis.editor.dto.EditorDocumentInfo;
import com.genesis.editor.dto.EditorSessionResponse;
import com.genesis.editor.dto.SaveSessionRequest;
import com.genesis.editor.dto.WorkspaceEditorResponse;
import com.genesis.editor.service.EditorService;
import com.genesis.importexport.service.ImportService;
import com.genesis.infra.storage.FileStorageService;
import com.genesis.workspace.service.DocumentService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for editor operations.
 * Provides APIs for the annotation editor UI.
 */
@RestController
@RequestMapping("/api/editor")
public class EditorController {

    private final EditorService editorService;
    private final ImportService importService;
    private final DocumentService documentService;
    private final FileStorageService fileStorageService;

    public EditorController(EditorService editorService,
            ImportService importService,
            DocumentService documentService,
            FileStorageService fileStorageService) {
        this.editorService = editorService;
        this.importService = importService;
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Open a workspace in the editor.
     * Returns session info, documents list, and aggregate stats.
     */
    @PostMapping("/workspaces/{workspaceId}/open")
    public ResponseEntity<ApiResponse<WorkspaceEditorResponse>> openWorkspace(
            @PathVariable UUID workspaceId,
            Principal principal) {
        UUID userId = getUserId(principal);
        WorkspaceEditorResponse response = editorService.openWorkspace(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all documents for a workspace with token counts.
     */
    @GetMapping("/workspaces/{workspaceId}/documents")
    public ResponseEntity<ApiResponse<List<EditorDocumentInfo>>> getWorkspaceDocuments(
            @PathVariable UUID workspaceId) {
        List<EditorDocumentInfo> documents = editorService.getWorkspaceDocuments(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * Get document content with tokens for display.
     */
    @GetMapping("/documents/{documentId}/content")
    public ResponseEntity<ApiResponse<DocumentContentResponse>> getDocumentContent(
            @PathVariable UUID documentId) {
        DocumentContentResponse response = editorService.getDocumentContent(documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get document content with workspace-level token offset.
     */
    @GetMapping("/workspaces/{workspaceId}/documents/{documentId}/content")
    public ResponseEntity<ApiResponse<DocumentContentResponse>> getDocumentContentWithOffset(
            @PathVariable UUID workspaceId,
            @PathVariable UUID documentId) {
        DocumentContentResponse response = editorService.getDocumentContentWithOffset(workspaceId, documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Tokenize a document (import plain text).
     */
    @PostMapping("/documents/{documentId}/tokenize")
    public ResponseEntity<ApiResponse<TokenizationResult>> tokenizeDocument(
            @PathVariable UUID documentId) throws Exception {
        // Get document content from storage
        var docInfo = documentService.getById(documentId);
        String content = fileStorageService.downloadAsString(docInfo.getStoredFileUrl());

        // Tokenize
        ImportService.ImportResult result = importService.importPlainText(documentId, content);

        TokenizationResult response = new TokenizationResult();
        response.setDocumentId(documentId);
        response.setSentenceCount(result.getSentenceCount());
        response.setTokenCount(result.getTokenCount());
        response.setSuccess(true);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current session state.
     */
    @GetMapping("/workspaces/{workspaceId}/session")
    public ResponseEntity<ApiResponse<EditorSessionResponse>> getSession(
            @PathVariable UUID workspaceId,
            Principal principal) {
        UUID userId = getUserId(principal);
        return editorService.getSession(workspaceId, userId)
                .map(session -> ResponseEntity.ok(ApiResponse.success(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save session state.
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<EditorSessionResponse>> saveSession(
            @RequestBody SaveSessionRequest request,
            Principal principal) {
        UUID userId = getUserId(principal);
        EditorSessionResponse response = editorService.saveSession(
                request.getWorkspaceId(),
                userId,
                request.getLastDocumentIndex(),
                request.getScrollPosition());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Close/clear session.
     */
    @DeleteMapping("/workspaces/{workspaceId}/session")
    public ResponseEntity<Void> closeSession(
            @PathVariable UUID workspaceId,
            Principal principal) {
        UUID userId = getUserId(principal);
        editorService.closeSession(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get tokenization status for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/status")
    public ResponseEntity<ApiResponse<WorkspaceTokenizationStatus>> getTokenizationStatus(
            @PathVariable UUID workspaceId) {
        List<EditorDocumentInfo> docs = editorService.getWorkspaceDocuments(workspaceId);

        WorkspaceTokenizationStatus status = new WorkspaceTokenizationStatus();
        status.setWorkspaceId(workspaceId);
        status.setTotalDocuments(docs.size());
        status.setTokenizedDocuments((int) docs.stream().filter(d -> Boolean.TRUE.equals(d.getIsTokenized())).count());
        status.setTotalTokens(docs.stream().mapToInt(d -> d.getTokenCount() != null ? d.getTokenCount() : 0).sum());
        status.setTotalSentences(
                docs.stream().mapToInt(d -> d.getSentenceCount() != null ? d.getSentenceCount() : 0).sum());
        status.setReady(
                status.getTokenizedDocuments().equals(status.getTotalDocuments()) && status.getTotalDocuments() > 0);

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    private UUID getUserId(Principal principal) {
        // In a real implementation, this would extract UUID from the principal
        // For now, use a deterministic UUID based on username
        return UUID.nameUUIDFromBytes(principal.getName().getBytes());
    }

    // Inner DTOs for API responses

    public static class TokenizationResult {
        private UUID documentId;
        private int sentenceCount;
        private int tokenCount;
        private boolean success;

        public UUID getDocumentId() {
            return documentId;
        }

        public void setDocumentId(UUID documentId) {
            this.documentId = documentId;
        }

        public int getSentenceCount() {
            return sentenceCount;
        }

        public void setSentenceCount(int sentenceCount) {
            this.sentenceCount = sentenceCount;
        }

        public int getTokenCount() {
            return tokenCount;
        }

        public void setTokenCount(int tokenCount) {
            this.tokenCount = tokenCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

    public static class WorkspaceTokenizationStatus {
        private UUID workspaceId;
        private Integer totalDocuments;
        private Integer tokenizedDocuments;
        private Integer totalTokens;
        private Integer totalSentences;
        private Boolean ready;

        public UUID getWorkspaceId() {
            return workspaceId;
        }

        public void setWorkspaceId(UUID workspaceId) {
            this.workspaceId = workspaceId;
        }

        public Integer getTotalDocuments() {
            return totalDocuments;
        }

        public void setTotalDocuments(Integer totalDocuments) {
            this.totalDocuments = totalDocuments;
        }

        public Integer getTokenizedDocuments() {
            return tokenizedDocuments;
        }

        public void setTokenizedDocuments(Integer tokenizedDocuments) {
            this.tokenizedDocuments = tokenizedDocuments;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        public Integer getTotalSentences() {
            return totalSentences;
        }

        public void setTotalSentences(Integer totalSentences) {
            this.totalSentences = totalSentences;
        }

        public Boolean getReady() {
            return ready;
        }

        public void setReady(Boolean ready) {
            this.ready = ready;
        }
    }
}
