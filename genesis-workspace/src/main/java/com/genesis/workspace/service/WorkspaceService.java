package com.genesis.workspace.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.dto.AddMemberRequest;
import com.genesis.workspace.dto.CreateWorkspaceRequest;
import com.genesis.workspace.dto.WorkspaceResponse;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.entity.WorkspaceStatus;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for workspace operations.
 */
@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new workspace.
     *
     * @param request the create request
     * @param ownerId the owner's user ID
     * @return the created workspace response
     */
    @Transactional
    public WorkspaceResponse create(@NonNull CreateWorkspaceRequest request, @NonNull UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        if (workspaceRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new ValidationException("Workspace with name '" + request.getName() + "' already exists");
        }

        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());
        workspace.setAnnotationType(request.getAnnotationType());
        workspace.setStatus(WorkspaceStatus.DRAFT);
        workspace.setOwner(owner);

        Workspace saved = workspaceRepository.save(workspace);
        return mapToResponse(saved);
    }

    /**
     * Get workspace by ID.
     *
     * @param workspaceId the workspace ID
     * @return the workspace response
     */
    public WorkspaceResponse getById(@NonNull UUID workspaceId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        return mapToResponse(workspace);
    }

    /**
     * Get all workspaces owned by a user.
     *
     * @param ownerId the owner's user ID
     * @return list of workspace responses
     */
    public List<WorkspaceResponse> getByOwnerId(@NonNull UUID ownerId) {
        return workspaceRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Add a member to a workspace.
     *
     * @param workspaceId the workspace ID
     * @param request     the add member request
     */
    @Transactional
    public void addMember(@NonNull UUID workspaceId, @NonNull AddMemberRequest request) {
        Workspace workspace = findWorkspaceById(workspaceId);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getUserId())) {
            throw new ValidationException("User is already a member of this workspace");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(request.getRole());

        workspaceMemberRepository.save(member);
    }

    /**
     * Remove a member from a workspace.
     *
     * @param workspaceId  the workspace ID
     * @param memberUserId the member's user ID
     */
    @Transactional
    public void removeMember(@NonNull UUID workspaceId, @NonNull UUID memberUserId) {
        findWorkspaceById(workspaceId); // Verify workspace exists

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, memberUserId)) {
            throw new ResourceNotFoundException("Member", memberUserId);
        }

        workspaceMemberRepository.deleteByWorkspaceIdAndUserId(workspaceId, memberUserId);
    }

    /**
     * Update workspace status.
     *
     * @param workspaceId the workspace ID
     * @param status      the new status
     * @return the updated workspace response
     */
    @Transactional
    public WorkspaceResponse updateStatus(@NonNull UUID workspaceId, @NonNull WorkspaceStatus status) {
        Workspace workspace = findWorkspaceById(workspaceId);
        workspace.setStatus(status);
        Workspace saved = workspaceRepository.save(workspace);
        return mapToResponse(saved);
    }

    /**
     * Delete a workspace.
     *
     * @param workspaceId the workspace ID
     */
    @Transactional
    public void delete(@NonNull UUID workspaceId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        workspaceRepository.delete(workspace);
    }

    private Workspace findWorkspaceById(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
    }

    private WorkspaceResponse mapToResponse(Workspace workspace) {
        WorkspaceResponse response = new WorkspaceResponse();
        response.setId(workspace.getId());
        response.setName(workspace.getName());
        response.setDescription(workspace.getDescription());
        response.setAnnotationType(workspace.getAnnotationType());
        response.setStatus(workspace.getStatus());
        response.setOwnerId(workspace.getOwner().getId());
        response.setOwnerUsername(workspace.getOwner().getUsername());
        response.setCreatedAt(workspace.getCreatedAt());
        response.setUpdatedAt(workspace.getUpdatedAt());
        return response;
    }
}
