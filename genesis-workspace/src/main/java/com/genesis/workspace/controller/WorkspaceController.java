package com.genesis.workspace.controller;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.dto.AddMemberRequest;
import com.genesis.workspace.dto.CreateWorkspaceRequest;
import com.genesis.workspace.dto.MemberResponse;
import com.genesis.workspace.dto.UpdateWorkspaceRequest;
import com.genesis.workspace.dto.WorkspaceResponse;
import com.genesis.workspace.entity.WorkspaceStatus;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for workspace operations.
 */
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    public WorkspaceController(WorkspaceService workspaceService, UserRepository userRepository) {
        this.workspaceService = workspaceService;
        this.userRepository = userRepository;
    }

    /**
     * Create a new workspace.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = getUserIdFromPrincipal(userDetails);
        WorkspaceResponse response = workspaceService.create(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Workspace created successfully"));
    }

    /**
     * Get workspace by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(@PathVariable UUID id) {
        WorkspaceResponse response = workspaceService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * List workspaces for the current user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listMyWorkspaces(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserIdFromPrincipal(userDetails);
        List<WorkspaceResponse> responses = workspaceService.getAllForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Update workspace status.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam WorkspaceStatus status) {
        WorkspaceResponse response = workspaceService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace status updated"));
    }

    /**
     * Update workspace details.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace updated successfully"));
    }

    /**
     * Delete a workspace.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        workspaceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Workspace deleted successfully"));
    }

    /**
     * Add a member to a workspace.
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        workspaceService.addMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Member added successfully"));
    }

    /**
     * Remove a member from a workspace.
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        workspaceService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }

    /**
     * Update a member's role.
     */
    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam MemberRole role) {
        workspaceService.updateMemberRole(id, userId, role);
        return ResponseEntity.ok(ApiResponse.success(null, "Member role updated successfully"));
    }

    /**
     * Get workspace members.
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(@PathVariable UUID id) {
        List<MemberResponse> responses = workspaceService.getMembers(id);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private UUID getUserIdFromPrincipal(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return user.getId();
    }
}
