package com.genesis.api.controller;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.wsd.dto.CreateSenseRequest;
import com.genesis.wsd.dto.CreateWsdAnnotationRequest;
import com.genesis.wsd.dto.WsdAnnotationDto;
import com.genesis.wsd.dto.WsdSenseDto;
import com.genesis.wsd.service.WsdAnnotationService;
import com.genesis.wsd.service.WsdExportService;
import com.genesis.wsd.service.WsdSenseService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for word-sense disambiguation:
 *  - sense inventory (admin CRUD)
 *  - per-annotator annotations (upsert / get / delete)
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/wsd")
public class WsdController {

    private final WsdSenseService senseService;
    private final WsdAnnotationService annotationService;
    private final WsdExportService exportService;
    private final UserRepository userRepository;

    public WsdController(WsdSenseService senseService,
            WsdAnnotationService annotationService,
            WsdExportService exportService,
            UserRepository userRepository) {
        this.senseService = senseService;
        this.annotationService = annotationService;
        this.exportService = exportService;
        this.userRepository = userRepository;
    }

    // ----- Sense CRUD -----

    @GetMapping("/senses")
    public ResponseEntity<ApiResponse<List<WsdSenseDto>>> listSenses(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String word) {
        UUID caller = currentUserId();
        List<WsdSenseDto> senses = (word == null || word.isBlank())
                ? senseService.listSenses(workspaceId, caller)
                : senseService.listSensesForWord(workspaceId, word, caller);
        return ResponseEntity.ok(ApiResponse.success(senses));
    }

    @PostMapping("/senses")
    public ResponseEntity<ApiResponse<WsdSenseDto>> createSense(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateSenseRequest request) {
        WsdSenseDto saved = senseService.createSense(workspaceId, currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/senses/{senseId}")
    public ResponseEntity<ApiResponse<WsdSenseDto>> updateSense(
            @PathVariable UUID workspaceId,
            @PathVariable UUID senseId,
            @Valid @RequestBody CreateSenseRequest request) {
        WsdSenseDto saved = senseService.updateSense(workspaceId, senseId, currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/senses/{senseId}")
    public ResponseEntity<ApiResponse<Void>> deleteSense(
            @PathVariable UUID workspaceId,
            @PathVariable UUID senseId) {
        senseService.deleteSense(workspaceId, senseId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ----- Annotation CRUD -----

    @PostMapping("/annotations")
    public ResponseEntity<ApiResponse<WsdAnnotationDto>> upsertAnnotation(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateWsdAnnotationRequest request) {
        UUID caller = currentUserId();
        WsdAnnotationDto saved = annotationService.upsert(
                workspaceId, caller, currentUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @GetMapping("/tokens/{tokenId}/annotations")
    public ResponseEntity<ApiResponse<List<WsdAnnotationDto>>> getAnnotationsForToken(
            @PathVariable UUID workspaceId,
            @PathVariable UUID tokenId) {
        List<WsdAnnotationDto> annotations = annotationService.getByToken(
                workspaceId, tokenId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    @DeleteMapping("/annotations/{annotationId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnotation(
            @PathVariable UUID workspaceId,
            @PathVariable UUID annotationId) {
        annotationService.deleteByAnnotator(
                workspaceId, annotationId, currentUsername(), currentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ----- Exports -----

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPerAnnotator(@PathVariable UUID workspaceId) {
        String tsv = exportService.exportPerAnnotator(workspaceId, currentUserId());
        return tsvResponse(tsv, "wsd_annotations_" + workspaceId + ".tsv");
    }

    @GetMapping("/export/consensus")
    public ResponseEntity<byte[]> exportConsensus(@PathVariable UUID workspaceId) {
        String tsv = exportService.exportConsensus(workspaceId, currentUserId());
        return tsvResponse(tsv, "wsd_consensus_" + workspaceId + ".tsv");
    }

    private ResponseEntity<byte[]> tsvResponse(String body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/tab-separated-values; charset=UTF-8"))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }

    // ----- helpers -----

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("Authentication required");
        }
        return auth.getName();
    }

    private UUID currentUserId() {
        String name = currentUsername();
        User user = userRepository.findByUsernameOrEmail(name, name)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + name));
        return user.getId();
    }
}
