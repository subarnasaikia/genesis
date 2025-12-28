package com.genesis.workspace.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.service.DocumentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for document operations.
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final com.genesis.user.repository.UserRepository userRepository;

    public DocumentController(DocumentService documentService,
            com.genesis.user.repository.UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    /**
     * Upload a document to a workspace.
     */
    @PostMapping("/workspaces/{workspaceId}/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        DocumentResponse response = documentService.upload(workspaceId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    // ... (listByWorkspace, getById, updateStatus remain same)

    /**
     * Delete a document.
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        documentService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    private UUID getUserId(org.springframework.security.core.userdetails.UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new com.genesis.common.exception.UnauthorizedException("User not found"))
                .getId();
    }

    /**
     * Get document count for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/documents/count")
    public ResponseEntity<ApiResponse<Long>> getCount(@PathVariable UUID workspaceId) {
        long count = documentService.countByWorkspaceId(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
