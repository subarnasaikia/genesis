package com.genesis.api.controller;

import com.genesis.api.security.AuthenticatedUserResolver;
import com.genesis.common.response.ApiResponse;
import com.genesis.common.response.CursorPage;
import com.genesis.coref.dto.ClusterDto;
import com.genesis.coref.dto.CreateClusterRequest;
import com.genesis.coref.dto.CreateMentionRequest;
import com.genesis.coref.dto.MentionDto;
import com.genesis.coref.dto.MergeClustersRequest;
import com.genesis.coref.service.ClusterService;
import com.genesis.coref.service.CoreferenceService;
import com.genesis.coref.service.MentionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
 * REST controller for coreference annotation operations.
 */
@RestController
@RequestMapping("/api")
public class CoreferenceController {

    private final MentionService mentionService;
    private final ClusterService clusterService;
    private final CoreferenceService coreferenceService;
    private final AuthenticatedUserResolver userResolver;
    private final com.genesis.workspace.service.DocumentService documentService;

    public CoreferenceController(MentionService mentionService,
            ClusterService clusterService,
            CoreferenceService coreferenceService,
            AuthenticatedUserResolver userResolver,
            com.genesis.workspace.service.DocumentService documentService) {
        this.mentionService = mentionService;
        this.clusterService = clusterService;
        this.coreferenceService = coreferenceService;
        this.userResolver = userResolver;
        this.documentService = documentService;
    }

    // ==================== Mention Endpoints ====================

    /**
     * Create a new mention.
     */
    @PostMapping("/workspaces/{workspaceId}/mentions")
    public ResponseEntity<ApiResponse<MentionDto>> createMention(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateMentionRequest request) {
        MentionDto mention = mentionService.createMention(workspaceId, request, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Get a cursor page of mentions for a workspace. Omit {@code cursor} for the
     * first page; pass the previous response's {@code nextCursor} for subsequent
     * pages. {@code limit} is clamped server-side.
     */
    @GetMapping("/workspaces/{workspaceId}/mentions")
    public ResponseEntity<ApiResponse<CursorPage<MentionDto>>> getMentionsByWorkspace(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "100") int limit) {
        CursorPage<MentionDto> page =
                mentionService.getMentionsByWorkspace(workspaceId, currentUserId(), cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Get all mentions for a document.
     */
    @GetMapping("/documents/{documentId}/mentions")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getMentionsByDocument(
            @PathVariable UUID documentId) {
        // Resolve the owning workspace in the composition root so coref doesn't
        // depend on DocumentService (A-001); the membership check stays in the service.
        UUID workspaceId = documentService.getByIdInternal(documentId).getWorkspaceId();
        List<MentionDto> mentions =
                mentionService.getMentionsByDocument(workspaceId, documentId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Get unassigned mentions for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/mentions/unassigned")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getUnassignedMentions(
            @PathVariable UUID workspaceId) {
        List<MentionDto> mentions = mentionService.getUnassignedMentions(workspaceId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Get mention by ID.
     */
    @GetMapping("/mentions/{mentionId}")
    public ResponseEntity<ApiResponse<MentionDto>> getMention(
            @PathVariable UUID mentionId) {
        MentionDto mention = mentionService.getMention(mentionId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Assign mention to cluster.
     */
    @PutMapping("/mentions/{mentionId}/cluster/{clusterId}")
    public ResponseEntity<ApiResponse<MentionDto>> assignToCluster(
            @PathVariable UUID mentionId,
            @PathVariable UUID clusterId) {
        MentionDto mention = mentionService.assignToCluster(mentionId, clusterId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Unassign mention from cluster.
     */
    @DeleteMapping("/mentions/{mentionId}/cluster")
    public ResponseEntity<ApiResponse<MentionDto>> unassignFromCluster(
            @PathVariable UUID mentionId) {
        MentionDto mention = mentionService.unassignFromCluster(mentionId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Delete mention.
     */
    @DeleteMapping("/mentions/{mentionId}")
    public ResponseEntity<Void> deleteMention(
            @PathVariable UUID mentionId) {
        mentionService.deleteMention(mentionId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    // ==================== Cluster Endpoints ====================

    /**
     * Create a new cluster.
     */
    @PostMapping("/workspaces/{workspaceId}/clusters")
    public ResponseEntity<ApiResponse<ClusterDto>> createCluster(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody(required = false) CreateClusterRequest request) {
        ClusterDto cluster = clusterService.createCluster(workspaceId, request, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    /**
     * Get a cursor page of clusters for a workspace. Omit {@code cursor} for the
     * first page; pass the previous response's {@code nextCursor} (the last
     * cluster number) for subsequent pages. {@code limit} is clamped server-side.
     */
    @GetMapping("/workspaces/{workspaceId}/clusters")
    public ResponseEntity<ApiResponse<CursorPage<ClusterDto>>> getClustersByWorkspace(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) Integer cursor,
            @RequestParam(defaultValue = "100") int limit) {
        CursorPage<ClusterDto> page =
                clusterService.getClusters(workspaceId, currentUserId(), cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Get cluster by ID.
     */
    @GetMapping("/clusters/{clusterId}")
    public ResponseEntity<ApiResponse<ClusterDto>> getCluster(
            @PathVariable UUID clusterId) {
        ClusterDto cluster = clusterService.getCluster(clusterId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    /**
     * Get mentions in a cluster.
     */
    @GetMapping("/clusters/{clusterId}/mentions")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getMentionsByCluster(
            @PathVariable UUID clusterId) {
        List<MentionDto> mentions = mentionService.getMentionsByCluster(clusterId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Update cluster.
     */
    @PutMapping("/clusters/{clusterId}")
    public ResponseEntity<ApiResponse<ClusterDto>> updateCluster(
            @PathVariable UUID clusterId,
            @Valid @RequestBody CreateClusterRequest request) {
        ClusterDto cluster = clusterService.updateCluster(clusterId, request, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    /**
     * Delete cluster (mentions are unassigned).
     */
    @DeleteMapping("/clusters/{clusterId}")
    public ResponseEntity<Void> deleteCluster(
            @PathVariable UUID clusterId) {
        clusterService.deleteCluster(clusterId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Merge one or more source clusters into a target cluster. The source
     * clusters' mentions are reassigned to the target, the source clusters are
     * deleted, and the remaining workspace clusters are renumbered contiguously.
     */
    @PostMapping("/workspaces/{workspaceId}/clusters/merge")
    public ResponseEntity<ApiResponse<ClusterDto>> mergeClusters(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody MergeClustersRequest request) {
        ClusterDto cluster = clusterService.mergeClusters(
                workspaceId,
                request != null ? request.getSourceClusterIds() : null,
                request != null ? request.getTargetClusterId() : null,
                currentUserId());
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    // ==================== Statistics Endpoints ====================

    /**
     * Get annotation statistics for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/coref/stats")
    public ResponseEntity<ApiResponse<CoreferenceService.AnnotationStats>> getStats(
            @PathVariable UUID workspaceId) {
        CoreferenceService.AnnotationStats stats = coreferenceService.getStats(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private UUID currentUserId() {
        return userResolver.currentUserId();
    }
}
