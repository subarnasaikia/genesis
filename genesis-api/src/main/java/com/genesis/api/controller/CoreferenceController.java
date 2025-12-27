package com.genesis.api.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.coref.dto.ClusterDto;
import com.genesis.coref.dto.CreateClusterRequest;
import com.genesis.coref.dto.CreateMentionRequest;
import com.genesis.coref.dto.MentionDto;
import com.genesis.coref.service.ClusterService;
import com.genesis.coref.service.CoreferenceService;
import com.genesis.coref.service.MentionService;
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

    public CoreferenceController(MentionService mentionService,
            ClusterService clusterService,
            CoreferenceService coreferenceService) {
        this.mentionService = mentionService;
        this.clusterService = clusterService;
        this.coreferenceService = coreferenceService;
    }

    // ==================== Mention Endpoints ====================

    /**
     * Create a new mention.
     */
    @PostMapping("/workspaces/{workspaceId}/mentions")
    public ResponseEntity<ApiResponse<MentionDto>> createMention(
            @PathVariable UUID workspaceId,
            @RequestBody CreateMentionRequest request) {
        MentionDto mention = mentionService.createMention(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Get all mentions for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/mentions")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getMentionsByWorkspace(
            @PathVariable UUID workspaceId) {
        List<MentionDto> mentions = mentionService.getMentionsByWorkspace(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Get all mentions for a document.
     */
    @GetMapping("/documents/{documentId}/mentions")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getMentionsByDocument(
            @PathVariable UUID documentId) {
        List<MentionDto> mentions = mentionService.getMentionsByDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Get unassigned mentions for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/mentions/unassigned")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getUnassignedMentions(
            @PathVariable UUID workspaceId) {
        List<MentionDto> mentions = mentionService.getUnassignedMentions(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Get mention by ID.
     */
    @GetMapping("/mentions/{mentionId}")
    public ResponseEntity<ApiResponse<MentionDto>> getMention(
            @PathVariable UUID mentionId) {
        MentionDto mention = mentionService.getMention(mentionId);
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Assign mention to cluster.
     */
    @PutMapping("/mentions/{mentionId}/cluster/{clusterId}")
    public ResponseEntity<ApiResponse<MentionDto>> assignToCluster(
            @PathVariable UUID mentionId,
            @PathVariable UUID clusterId) {
        MentionDto mention = mentionService.assignToCluster(mentionId, clusterId);
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Unassign mention from cluster.
     */
    @DeleteMapping("/mentions/{mentionId}/cluster")
    public ResponseEntity<ApiResponse<MentionDto>> unassignFromCluster(
            @PathVariable UUID mentionId) {
        MentionDto mention = mentionService.unassignFromCluster(mentionId);
        return ResponseEntity.ok(ApiResponse.success(mention));
    }

    /**
     * Delete mention.
     */
    @DeleteMapping("/mentions/{mentionId}")
    public ResponseEntity<Void> deleteMention(
            @PathVariable UUID mentionId) {
        mentionService.deleteMention(mentionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Cluster Endpoints ====================

    /**
     * Create a new cluster.
     */
    @PostMapping("/workspaces/{workspaceId}/clusters")
    public ResponseEntity<ApiResponse<ClusterDto>> createCluster(
            @PathVariable UUID workspaceId,
            @RequestBody(required = false) CreateClusterRequest request) {
        ClusterDto cluster = clusterService.createCluster(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    /**
     * Get all clusters for a workspace.
     */
    @GetMapping("/workspaces/{workspaceId}/clusters")
    public ResponseEntity<ApiResponse<List<ClusterDto>>> getClustersByWorkspace(
            @PathVariable UUID workspaceId) {
        List<ClusterDto> clusters = clusterService.getClusters(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(clusters));
    }

    /**
     * Get cluster by ID.
     */
    @GetMapping("/clusters/{clusterId}")
    public ResponseEntity<ApiResponse<ClusterDto>> getCluster(
            @PathVariable UUID clusterId) {
        ClusterDto cluster = clusterService.getCluster(clusterId);
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    /**
     * Get mentions in a cluster.
     */
    @GetMapping("/clusters/{clusterId}/mentions")
    public ResponseEntity<ApiResponse<List<MentionDto>>> getMentionsByCluster(
            @PathVariable UUID clusterId) {
        List<MentionDto> mentions = mentionService.getMentionsByCluster(clusterId);
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }

    /**
     * Update cluster.
     */
    @PutMapping("/clusters/{clusterId}")
    public ResponseEntity<ApiResponse<ClusterDto>> updateCluster(
            @PathVariable UUID clusterId,
            @RequestBody CreateClusterRequest request) {
        ClusterDto cluster = clusterService.updateCluster(clusterId, request);
        return ResponseEntity.ok(ApiResponse.success(cluster));
    }

    /**
     * Delete cluster (mentions are unassigned).
     */
    @DeleteMapping("/clusters/{clusterId}")
    public ResponseEntity<Void> deleteCluster(
            @PathVariable UUID clusterId) {
        clusterService.deleteCluster(clusterId);
        return ResponseEntity.noContent().build();
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
}
