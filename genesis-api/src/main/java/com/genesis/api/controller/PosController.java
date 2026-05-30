package com.genesis.api.controller;

import com.genesis.api.security.AuthenticatedUserResolver;
import com.genesis.common.exception.UnauthorizedException;
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
import org.springframework.util.StringUtils;
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
    private final AuthenticatedUserResolver userResolver;

    public PosController(PosTaggingService posTaggingService,
            AuthenticatedUserResolver userResolver) {
        this.posTaggingService = posTaggingService;
        this.userResolver = userResolver;
    }

    /**
     * Set or clear the current annotator's POS tag for a token.
     */
    @PutMapping("/tokens/{tokenId}/pos")
    public ResponseEntity<ApiResponse<PosAnnotationDto>> updatePos(
            @PathVariable UUID tokenId,
            @Valid @RequestBody UpdatePosRequest request) {
        String annotator = currentAnnotator();
        UUID callerId = currentUserId();
        PosAnnotationDto dto = posTaggingService.updatePos(tokenId, callerId, annotator,
                request != null ? request.getPos() : null);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Batch upsert/clear POS tags for the current annotator.
     */
    @PutMapping("/tokens/pos/batch")
    public ResponseEntity<ApiResponse<List<PosAnnotationDto>>> batchUpdatePos(
            @Valid @RequestBody BatchUpdatePosRequest request) {
        String annotator = currentAnnotator();
        UUID callerId = currentUserId();
        List<PosAnnotationDto> result = posTaggingService.batchUpdate(
                request != null ? request.getUpdates() : null,
                callerId,
                annotator);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * All annotators' POS tags for a token.
     */
    @GetMapping("/tokens/{tokenId}/pos")
    public ResponseEntity<ApiResponse<List<PosAnnotationDto>>> getAnnotationsForToken(
            @PathVariable UUID tokenId) {
        UUID callerId = currentUserId();
        List<PosAnnotationDto> annotations = posTaggingService.getAnnotationsByToken(tokenId, callerId);
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    /**
     * All POS annotations for every token in a document, across all annotators.
     */
    @GetMapping("/documents/{documentId}/pos")
    public ResponseEntity<ApiResponse<List<PosAnnotationDto>>> getAnnotationsForDocument(
            @PathVariable UUID documentId) {
        UUID callerId = currentUserId();
        List<PosAnnotationDto> annotations = posTaggingService.getAnnotationsByDocument(documentId, callerId);
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    /**
     * Token-id → majority POS map for the document.
     */
    @GetMapping("/documents/{documentId}/pos/majority")
    public ResponseEntity<ApiResponse<Map<UUID, String>>> getMajorityPosForDocument(
            @PathVariable UUID documentId) {
        UUID callerId = currentUserId();
        Map<UUID, String> majority = posTaggingService.getMajorityPosByDocument(documentId, callerId);
        return ResponseEntity.ok(ApiResponse.success(majority));
    }

    /**
     * Resolves the authenticated annotator's username. Throws if there is no
     * authenticated principal — there is no {@code "system"} fallback because
     * writes must never persist under a synthetic annotator if the JWT filter
     * is bypassed (SECURITY_AUDIT MEDIUM-4).
     */
    private String currentAnnotator() {
        String name = userResolver.currentUsername();
        if (!StringUtils.hasText(name)) {
            throw new UnauthorizedException("User not authenticated");
        }
        return name;
    }

    private UUID currentUserId() {
        return userResolver.currentUserId();
    }
}
