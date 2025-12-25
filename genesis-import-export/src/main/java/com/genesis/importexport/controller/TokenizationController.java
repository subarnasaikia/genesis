package com.genesis.importexport.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.importexport.service.DocumentTextService;
import com.genesis.importexport.service.TextTokenizationService;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for document tokenization operations.
 */
@RestController
@RequestMapping("/api/tokenization")
public class TokenizationController {

    private final TextTokenizationService tokenizationService;
    private final DocumentTextService documentTextService;
    private final DocumentRepository documentRepository;

    public TokenizationController(
            TextTokenizationService tokenizationService,
            DocumentTextService documentTextService,
            DocumentRepository documentRepository) {
        this.tokenizationService = tokenizationService;
        this.documentTextService = documentTextService;
        this.documentRepository = documentRepository;
    }

    /**
     * Tokenize a document by downloading its content from Cloudinary and
     * saving tokens to the database.
     *
     * @param documentId the document ID
     * @return success response with token count
     */
    @PostMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Integer>> tokenizeDocument(
            @PathVariable UUID documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (document.getStoredFile() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Document has no uploaded file"));
        }

        // Download text content from Cloudinary URL
        String text = documentTextService.extractTextFromUrl(document.getStoredFile().getUrl());

        // Tokenize and save
        var tokens = tokenizationService.tokenizeAndSave(documentId, text);

        return ResponseEntity.ok(
                ApiResponse.success(tokens.size(),
                        String.format("Document tokenized into %d tokens", tokens.size())));
    }

    /**
     * Tokenize all documents in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return success response with total token count
     */
    @PostMapping("/workspaces/{workspaceId}")
    public ResponseEntity<ApiResponse<Integer>> tokenizeWorkspace(
            @PathVariable UUID workspaceId) {

        int tokenCount = tokenizationService.tokenizeWorkspace(workspaceId);

        return ResponseEntity.ok(
                ApiResponse.success(tokenCount,
                        String.format("Workspace tokenized into %d tokens", tokenCount)));
    }

    /**
     * Get token count for a document.
     *
     * @param documentId the document ID
     * @return token count
     */
    @GetMapping("/documents/{documentId}/count")
    public ResponseEntity<ApiResponse<Long>> getTokenCount(@PathVariable UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }

        long count = tokenizationService.getTokensForDocument(documentId).size();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
