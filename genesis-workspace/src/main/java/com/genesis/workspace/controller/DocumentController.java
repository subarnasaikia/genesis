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

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload a document to a workspace.
     */
    @PostMapping("/workspaces/{workspaceId}/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file) {
        DocumentResponse response = documentService.upload(workspaceId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    /**
     * List documents in a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listByWorkspace(
            @PathVariable UUID workspaceId) {
        List<DocumentResponse> responses = documentService.getByWorkspaceId(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get document by ID.
     */
    @GetMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(@PathVariable UUID id) {
        DocumentResponse response = documentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update document status.
     */
    @PutMapping("/documents/{id}/status")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam DocumentStatus status) {
        DocumentResponse response = documentService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Document status updated"));
    }

    /**
     * Delete a document.
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
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
