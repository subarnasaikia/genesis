package com.genesis.api.controller;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.coref.service.CoreferenceService;
import com.genesis.importexport.dto.ExportOptions;
import com.genesis.importexport.service.ExportService;
import com.genesis.importexport.service.ExportService.DocumentInfo;
import com.genesis.importexport.service.ExportService.ExportResult;
import com.genesis.infra.security.JwtTokenProvider;
import com.genesis.pos.service.PosTaggingService;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.service.DocumentService;
import com.genesis.workspace.service.WorkspaceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Share-link export.
 *
 * <p>POST {@code /api/workspaces/{id}/export/share} issues a 24-hour
 * JWT scoped to a single workspace via the {@code workspace_id} claim.
 * The token is then redeemable (without a logged-in session) at
 * {@code GET /api/public/export/conll/{workspaceId}?token=<jwt>} —
 * the controller cross-checks the path workspaceId against the claim
 * so a token can't be used to download a different workspace.
 *
 * <p>The {@code token} query param is redacted by
 * {@code RequestLoggingInterceptor} so share links don't leak into
 * access logs (eng-review D8).
 */
@RestController
public class ShareExportController {

    /** 24-hour lifetime for share tokens. */
    private static final long SHARE_TOKEN_EXPIRY_MS = Duration.ofHours(24).toMillis();

    private static final String CLAIM_WORKSPACE_ID = "workspace_id";

    private final JwtTokenProvider jwtTokenProvider;
    private final ExportService exportService;
    private final WorkspaceService workspaceService;
    private final DocumentService documentService;
    private final CoreferenceService coreferenceService;
    private final PosTaggingService posTaggingService;

    public ShareExportController(JwtTokenProvider jwtTokenProvider,
            ExportService exportService,
            WorkspaceService workspaceService,
            DocumentService documentService,
            CoreferenceService coreferenceService,
            PosTaggingService posTaggingService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.exportService = exportService;
        this.workspaceService = workspaceService;
        this.documentService = documentService;
        this.coreferenceService = coreferenceService;
        this.posTaggingService = posTaggingService;
    }

    /**
     * Authenticated endpoint — returns a fresh 24h share token.
     */
    @PostMapping("/api/workspaces/{workspaceId}/export/share")
    public ResponseEntity<ApiResponse<ShareTokenResponse>> issueShareToken(
            @PathVariable UUID workspaceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_WORKSPACE_ID, workspaceId.toString());

        String token = jwtTokenProvider.generateToken(claims, SHARE_TOKEN_EXPIRY_MS);
        return ResponseEntity.ok(ApiResponse.success(
                new ShareTokenResponse(token, SHARE_TOKEN_EXPIRY_MS / 1000L)));
    }

    /**
     * Public, token-gated endpoint. The path is allow-listed in
     * {@code SecurityConfig} — auth is solely via the JWT in the query.
     */
    @GetMapping("/api/public/export/conll/{workspaceId}")
    public ResponseEntity<byte[]> downloadWithShareToken(
            @PathVariable UUID workspaceId,
            @RequestParam("token") String token) throws IOException {
        Claims claims;
        try {
            claims = jwtTokenProvider.getClaims(token);
        } catch (JwtException ex) {
            throw new UnauthorizedException("Invalid or expired share token");
        }

        String claimWorkspaceId = claims.get(CLAIM_WORKSPACE_ID, String.class);
        if (claimWorkspaceId == null || !claimWorkspaceId.equals(workspaceId.toString())) {
            throw new UnauthorizedException("Token does not grant access to this workspace");
        }

        ExportOptions options = new ExportOptions();
        var workspace = workspaceService.getById(workspaceId);
        List<DocumentResponse> documents = documentService.getByWorkspaceId(workspaceId);

        List<DocumentInfo> docInfos = documents.stream()
                .map(d -> new DocumentInfo(d.getId(), d.getName()))
                .collect(Collectors.toList());

        Map<UUID, Map<String, String>> corefAnnotationsPerDoc = coreferenceService
                .generateWorkspaceCorefAnnotations(workspaceId);
        Map<UUID, Map<UUID, String>> posOverridesPerDoc = new HashMap<>();
        Map<UUID, Map<UUID, Long>> annotatorCountsPerDoc = new HashMap<>();
        for (DocumentResponse d : documents) {
            posOverridesPerDoc.put(d.getId(), posTaggingService.getMajorityPosByDocument(d.getId()));
            annotatorCountsPerDoc.put(d.getId(), posTaggingService.getAnnotatorCountsByDocument(d.getId()));
        }

        ExportResult result = exportService.exportWorkspace(
                docInfos,
                corefAnnotationsPerDoc,
                posOverridesPerDoc,
                annotatorCountsPerDoc,
                options,
                workspace.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getContent());
    }

    /** Response envelope for the share-token endpoint. */
    public record ShareTokenResponse(String token, long expiresInSeconds) {}
}
