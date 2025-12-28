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
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.WorkspaceStatus;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import com.genesis.workspace.dto.MemberResponse;
import com.genesis.workspace.dto.UpdateWorkspaceRequest;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.repository.DocumentRepository;
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
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentService documentService,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.eventPublisher = eventPublisher;
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

        // Add owner as a member with ADMIN role
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(saved);
        member.setUser(owner);
        member.setRole(MemberRole.ADMIN);
        workspaceMemberRepository.save(member);

        eventPublisher.publishEvent(
                new com.genesis.workspace.event.WorkspaceCreatedEvent(this, saved.getId(), saved.getName(), ownerId));

        return mapToResponse(saved);
    }

    /**
     * Update workspace details.
     *
     * @param id      the workspace ID
     * @param request the update request
     * @return the updated workspace response
     */
    @Transactional
    public WorkspaceResponse update(@NonNull UUID id, @NonNull UpdateWorkspaceRequest request) {
        Workspace workspace = findWorkspaceById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!workspace.getName().equals(request.getName())
                    && workspaceRepository.existsByNameAndOwnerId(request.getName(), workspace.getOwner().getId())) {
                throw new ValidationException("Workspace with name '" + request.getName() + "' already exists");
            }
            workspace.setName(request.getName());
        }

        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

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
     * Get all workspaces the user is a member of.
     *
     * @param userId the user ID
     * @return list of workspace responses
     */
    public List<WorkspaceResponse> getAllForUser(@NonNull UUID userId) {
        return workspaceRepository.findByMemberUserId(userId).stream()
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
    public void addMember(@NonNull UUID workspaceId, @NonNull AddMemberRequest request, @NonNull UUID actorId) {
        Workspace workspace = findWorkspaceById(workspaceId);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User with email " + request.getEmail() + " not found"));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new ValidationException("User is already a member of this workspace");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(request.getRole());

        workspaceMemberRepository.save(member);

        eventPublisher.publishEvent(new com.genesis.workspace.event.MemberAddedEvent(this, workspaceId,
                workspace.getName(), user.getId(), actorId));
    }

    /**
     * Remove a member from a workspace.
     *
     * @param workspaceId  the workspace ID
     * @param memberUserId the member's user ID
     */
    @Transactional
    public void removeMember(@NonNull UUID workspaceId, @NonNull UUID memberUserId) {
        Workspace workspace = findWorkspaceById(workspaceId);

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, memberUserId)) {
            throw new ResourceNotFoundException("Member", memberUserId);
        }

        workspaceMemberRepository.deleteByWorkspaceIdAndUserId(workspaceId, memberUserId);

        // Publish event for notification
        eventPublisher.publishEvent(new com.genesis.workspace.event.MemberRemovedEvent(
                this,
                workspaceId,
                workspace.getName(),
                memberUserId,
                null // Actor ID can be added if passed to this method
        ));
    }

    /**
     * Update a member's role.
     *
     * @param workspaceId  the workspace ID
     * @param memberUserId the member's user ID
     * @param role         the new role
     */
    @Transactional
    public void updateMemberRole(@NonNull UUID workspaceId, @NonNull UUID memberUserId, @NonNull MemberRole role) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberUserId));

        if (member.getWorkspace().getOwner().getId().equals(memberUserId)) {
            throw new ValidationException("Cannot change role of the workspace owner");
        }

        member.setRole(role);
        workspaceMemberRepository.save(member);
    }

    /**
     * Get all members of a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of member responses
     */
    public List<MemberResponse> getMembers(@NonNull UUID workspaceId) {
        findWorkspaceById(workspaceId); // Verify workspace exists

        return workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    private MemberResponse mapToMemberResponse(WorkspaceMember member) {
        MemberResponse response = new MemberResponse();
        User user = member.getUser();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(member.getRole());
        return response;
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
    public void delete(@NonNull UUID workspaceId, @NonNull UUID userId) {
        Workspace workspace = findWorkspaceById(workspaceId);

        // Get members to notify before deleting
        List<UUID> memberIds = workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toList());
        String workspaceName = workspace.getName();

        // Delete all members
        workspaceMemberRepository.deleteByWorkspaceId(workspaceId);

        // Delete all documents (handles file storage cleanup)
        List<Document> documents = documentRepository.findByWorkspaceId(workspaceId);
        for (Document doc : documents) {
            documentService.delete(doc.getId(), userId);
        }

        workspaceRepository.delete(workspace);

        eventPublisher.publishEvent(
                new com.genesis.workspace.event.WorkspaceDeletedEvent(this, workspaceId, workspaceName, memberIds));
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

        // Calculate progress stats
        long totalDocs = documentRepository.countByWorkspaceId(workspace.getId());
        long completedDocs = documentRepository.countByWorkspaceIdAndStatus(workspace.getId(), DocumentStatus.COMPLETE);

        response.setDocumentCount(totalDocs);
        response.setAnnotatedDocumentCount(completedDocs);
        if (totalDocs > 0) {
            response.setProgressPercentage((int) ((completedDocs * 100) / totalDocs));
        } else {
            response.setProgressPercentage(0);
        }

        return response;
    }
}
