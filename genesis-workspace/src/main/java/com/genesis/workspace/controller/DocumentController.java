package com.genesis.workspace.controller;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.service.DocumentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("/workspaces/{workspaceId}/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        DocumentResponse response = documentService.upload(workspaceId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listByWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserId(userDetails);
        List<DocumentResponse> responses = documentService.getByWorkspaceId(workspaceId, callerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserId(userDetails);
        DocumentResponse response = documentService.getById(id, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/documents/{id}/status")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam DocumentStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserId(userDetails);
        DocumentResponse response = documentService.updateStatus(id, status, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document status updated"));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        documentService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    @GetMapping("/workspaces/{workspaceId}/documents/count")
    public ResponseEntity<ApiResponse<Long>> getCount(@PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserId(userDetails);
        long count = documentService.countByWorkspaceId(workspaceId, callerId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    private UUID getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"))
                .getId();
    }
}
