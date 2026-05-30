package com.genesis.workspace.controller;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.user.service.UserService;
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
    private final UserService userService;

    public WorkspaceController(WorkspaceService workspaceService, UserService userService) {
        this.workspaceService = workspaceService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = getUserIdFromPrincipal(userDetails);
        WorkspaceResponse response = workspaceService.create(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Workspace created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserIdFromPrincipal(userDetails);
        WorkspaceResponse response = workspaceService.getById(id, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listMyWorkspaces(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserIdFromPrincipal(userDetails);
        List<WorkspaceResponse> responses = workspaceService.getAllForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam WorkspaceStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserIdFromPrincipal(userDetails);
        WorkspaceResponse response = workspaceService.updateStatus(id, status, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace status updated"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserIdFromPrincipal(userDetails);
        WorkspaceResponse response = workspaceService.update(id, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserIdFromPrincipal(userDetails);
        workspaceService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Workspace deleted successfully"));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = getUserIdFromPrincipal(userDetails);
        workspaceService.addMember(id, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Member added successfully"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = getUserIdFromPrincipal(userDetails);
        workspaceService.removeMember(id, userId, actorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }

    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam MemberRole role,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = getUserIdFromPrincipal(userDetails);
        workspaceService.updateMemberRole(id, userId, role, actorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member role updated successfully"));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID callerId = getUserIdFromPrincipal(userDetails);
        List<MemberResponse> responses = workspaceService.getMembers(id, callerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private UUID getUserIdFromPrincipal(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return userService.getUserIdByUsername(userDetails.getUsername());
    }
}
