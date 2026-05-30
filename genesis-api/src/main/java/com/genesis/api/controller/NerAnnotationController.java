package com.genesis.api.controller;

import com.genesis.api.security.AuthenticatedUserResolver;
import com.genesis.common.response.ApiResponse;
import com.genesis.ner.dto.CreateNerAnnotationRequest;
import com.genesis.ner.dto.NerAnnotationDto;
import com.genesis.ner.dto.UpdateNerAnnotationRequest;
import com.genesis.ner.service.NerAnnotationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Span-level NER annotation endpoints. Each row covers a token range
 * [start..end] (inclusive global index) carrying a label from the workspace's
 * effective tag set. Overlapping/nested spans are intentionally allowed.
 */
@RestController
@RequestMapping("/api/ner-annotations")
public class NerAnnotationController {

    private final NerAnnotationService annotationService;
    private final AuthenticatedUserResolver userResolver;

    public NerAnnotationController(NerAnnotationService annotationService,
            AuthenticatedUserResolver userResolver) {
        this.annotationService = annotationService;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NerAnnotationDto>> create(
            @Valid @RequestBody CreateNerAnnotationRequest request) {
        NerAnnotationDto created = annotationService.create(request, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PatchMapping("/{annotationId}")
    public ResponseEntity<ApiResponse<NerAnnotationDto>> update(
            @PathVariable UUID annotationId,
            @Valid @RequestBody UpdateNerAnnotationRequest request) {
        NerAnnotationDto updated = annotationService.update(annotationId, request, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{annotationId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID annotationId) {
        annotationService.delete(annotationId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NerAnnotationDto>>> list(
            @RequestParam("documentId") UUID documentId,
            @RequestParam(value = "annotatorId", required = false) String annotatorId) {
        UUID callerId = currentUserId();
        List<NerAnnotationDto> result = (annotatorId == null || annotatorId.isBlank())
                ? annotationService.listByDocument(documentId, callerId)
                : annotationService.listByDocumentAndAnnotator(documentId, annotatorId, callerId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private UUID currentUserId() {
        return userResolver.currentUserId();
    }
}
