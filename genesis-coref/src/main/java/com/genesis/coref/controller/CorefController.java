package com.genesis.coref.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.coref.dto.ClusterResponse;
import com.genesis.coref.dto.CreateMentionRequest;
import com.genesis.coref.dto.MentionResponse;
import com.genesis.coref.entity.Cluster;
import com.genesis.coref.entity.Mention;
import com.genesis.coref.service.CorefService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for coreference annotation operations.
 */
@RestController
@RequestMapping("/api/coref")
public class CorefController {

    private final CorefService corefService;

    public CorefController(CorefService corefService) {
        this.corefService = corefService;
    }

    /**
     * Create a new coreference cluster in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return the created cluster
     */
    @PostMapping("/workspaces/{workspaceId}/clusters")
    public ResponseEntity<ApiResponse<ClusterResponse>> createCluster(
            @PathVariable UUID workspaceId) {

        Cluster cluster = corefService.createCluster(workspaceId);
        ClusterResponse response = ClusterResponse.fromEntity(cluster);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Cluster created successfully"));
    }

    /**
     * Get all clusters for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of clusters
     */
    @GetMapping("/workspaces/{workspaceId}/clusters")
    public ResponseEntity<ApiResponse<List<ClusterResponse>>> getClusters(
            @PathVariable UUID workspaceId) {

        List<Cluster> clusters = corefService.getClustersForWorkspace(workspaceId);
        List<ClusterResponse> responses = clusters.stream()
                .map(ClusterResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Add a mention to a cluster.
     *
     * @param clusterId the cluster ID
     * @param request   the mention creation request
     * @return the created mention
     */
    @PostMapping("/clusters/{clusterId}/mentions")
    public ResponseEntity<ApiResponse<MentionResponse>> addMention(
            @PathVariable UUID clusterId,
            @Valid @RequestBody CreateMentionRequest request) {

        Mention mention = corefService.addMentionToCluster(
                clusterId,
                request.getTokenStartIndex(),
                request.getTokenEndIndex());

        MentionResponse response = MentionResponse.fromEntity(mention);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Mention added successfully"));
    }

    /**
     * Get all mentions for a cluster.
     *
     * @param clusterId the cluster ID
     * @return list of mentions
     */
    @GetMapping("/clusters/{clusterId}/mentions")
    public ResponseEntity<ApiResponse<List<MentionResponse>>> getMentions(
            @PathVariable UUID clusterId) {

        List<Mention> mentions = corefService.getMentionsForCluster(clusterId);
        List<MentionResponse> responses = mentions.stream()
                .map(MentionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Delete a mention.
     *
     * @param mentionId the mention ID
     * @return success response
     */
    @DeleteMapping("/mentions/{mentionId}")
    public ResponseEntity<ApiResponse<Void>> deleteMention(@PathVariable UUID mentionId) {
        corefService.deleteMention(mentionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Mention deleted successfully"));
    }

    /**
     * Delete a cluster and all its mentions.
     *
     * @param clusterId the cluster ID
     * @return success response
     */
    @DeleteMapping("/clusters/{clusterId}")
    public ResponseEntity<ApiResponse<Void>> deleteCluster(@PathVariable UUID clusterId) {
        corefService.deleteCluster(clusterId);
        return ResponseEntity.ok(ApiResponse.success(null, "Cluster deleted successfully"));
    }

    /**
     * Delete all annotations for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return success response
     */
    @DeleteMapping("/workspaces/{workspaceId}/annotations")
    public ResponseEntity<ApiResponse<Void>> deleteAllAnnotations(
            @PathVariable UUID workspaceId) {
        corefService.deleteAllAnnotations(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(null, "All annotations deleted successfully"));
    }
}
