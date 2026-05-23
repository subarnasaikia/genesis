package com.genesis.api.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.pos.dto.BatchUpdatePosRequest;
import com.genesis.pos.dto.PosAnnotationDto;
import com.genesis.pos.dto.UpdatePosRequest;
import com.genesis.pos.service.PosTaggingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for part-of-speech annotation operations.
 *
 * <p>Each annotator gets their own row per token in {@code pos_annotations}.
 * Majority across all annotators is computed at export time (CoNLL column 5).
 */
@RestController
@RequestMapping("/api")
public class PosController {

    private final PosTaggingService posTaggingService;

    public PosController(PosTaggingService posTaggingService) {
        this.posTaggingService = posTaggingService;
    }

    /**
     * Set or clear the current annotator's POS tag for a token.
     */
    @PutMapping("/tokens/{tokenId}/pos")
    public ResponseEntity<ApiResponse<PosAnnotationDto>> updatePos(
            @PathVariable UUID tokenId,
            @Valid @RequestBody UpdatePosRequest request) {
        PosAnnotationDto dto = posTaggingService.updatePos(tokenId, currentAnnotator(),
                request != null ? request.getPos() : null);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Batch upsert/clear POS tags for the current annotator.
     */
    @PutMapping("/tokens/pos/batch")
    public ResponseEntity<ApiResponse<List<PosAnnotationDto>>> batchUpdatePos(
            @Valid @RequestBody BatchUpdatePosRequest request) {
        List<PosAnnotationDto> result = posTaggingService.batchUpdate(
                request != null ? request.getUpdates() : null,
                currentAnnotator());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * All annotators' POS tags for a token.
     */
    @GetMapping("/tokens/{tokenId}/pos")
    public ResponseEntity<ApiResponse<List<PosAnnotationDto>>> getAnnotationsForToken(
            @PathVariable UUID tokenId) {
        List<PosAnnotationDto> annotations = posTaggingService.getAnnotationsByToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    /**
     * All POS annotations for every token in a document, across all annotators.
     */
    @GetMapping("/documents/{documentId}/pos")
    public ResponseEntity<ApiResponse<List<PosAnnotationDto>>> getAnnotationsForDocument(
            @PathVariable UUID documentId) {
        List<PosAnnotationDto> annotations = posTaggingService.getAnnotationsByDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    /**
     * Token-id → majority POS map for the document.
     */
    @GetMapping("/documents/{documentId}/pos/majority")
    public ResponseEntity<ApiResponse<Map<UUID, String>>> getMajorityPosForDocument(
            @PathVariable UUID documentId) {
        Map<UUID, String> majority = posTaggingService.getMajorityPosByDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(majority));
    }

    private String currentAnnotator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }
}
