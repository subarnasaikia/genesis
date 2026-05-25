package com.genesis.pos.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.pos.dto.CreatePosTagRequest;
import com.genesis.pos.dto.PosTagDefinitionDto;
import com.genesis.pos.entity.PosTagDefinitionEntity;
import com.genesis.pos.entity.PosTagScope;
import com.genesis.pos.repository.PosTagDefinitionRepository;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import com.genesis.workspace.service.WorkspaceAccessControl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PosTagDefinitionService {

    private static final Pattern TAG_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,19}$");

    private final PosTagDefinitionRepository definitionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceAccessControl accessControl;

    public PosTagDefinitionService(PosTagDefinitionRepository definitionRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            WorkspaceAccessControl accessControl) {
        this.definitionRepository = definitionRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.accessControl = accessControl;
    }

    public PosTagDefinitionDto create(CreatePosTagRequest request, UUID callerUserId) {
        if (request == null) {
            throw new ValidationException("body", "Request body required");
        }
        if (callerUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        String tag = request.getTag() == null ? null : request.getTag().trim();
        if (tag == null || tag.isEmpty()) {
            throw new ValidationException("tag", "Tag is required");
        }
        if (!TAG_PATTERN.matcher(tag).matches()) {
            throw new ValidationException("tag",
                    "Tag must start with uppercase letter and contain only A-Z, 0-9, underscore (max 20 chars)");
        }
        if (PosTaggingService.UNIVERSAL_POS_TAGS.contains(tag)) {
            throw new ValidationException("tag", "Tag conflicts with a built-in universal tag: " + tag);
        }
        PosTagScope scope = request.getScope();
        if (scope == null) {
            throw new ValidationException("scope", "Scope is required (GLOBAL or WORKSPACE)");
        }

        UUID workspaceId = null;
        if (scope == PosTagScope.WORKSPACE) {
            workspaceId = request.getWorkspaceId();
            if (workspaceId == null) {
                throw new ValidationException("workspaceId",
                        "workspaceId is required when scope=WORKSPACE");
            }
            requireWorkspaceAdmin(workspaceId, callerUserId);
            definitionRepository.findByTagAndScopeAndWorkspaceId(tag, scope, workspaceId)
                    .ifPresent(existing -> {
                        throw new ValidationException("tag",
                                "Tag already exists in this workspace: " + tag);
                    });
        } else {
            definitionRepository.findByTagAndScopeAndWorkspaceIdIsNull(tag, scope)
                    .ifPresent(existing -> {
                        throw new ValidationException("tag",
                                "Global tag already exists: " + tag);
                    });
        }

        PosTagDefinitionEntity entity = new PosTagDefinitionEntity();
        entity.setTag(tag);
        entity.setDescription(request.getDescription());
        entity.setScope(scope);
        entity.setWorkspaceId(workspaceId);
        entity.setCreatedByUserId(callerUserId.toString());

        return PosTagDefinitionDto.from(definitionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PosTagDefinitionDto> listForWorkspace(UUID workspaceId, UUID callerUserId) {
        if (workspaceId != null) {
            accessControl.requireMember(workspaceId, callerUserId);
        }
        List<PosTagDefinitionDto> result = new ArrayList<>();
        for (String tag : PosTaggingService.UNIVERSAL_POS_TAGS) {
            result.add(PosTagDefinitionDto.builtin(tag));
        }
        definitionRepository.findByScope(PosTagScope.GLOBAL)
                .forEach(e -> result.add(PosTagDefinitionDto.from(e)));
        if (workspaceId != null) {
            definitionRepository.findByWorkspaceId(workspaceId)
                    .forEach(e -> result.add(PosTagDefinitionDto.from(e)));
        }
        return result;
    }

    /**
     * Effective tag set used by {@code PosTaggingService} to validate POS
     * annotations. Includes universal tags + global customs + workspace customs.
     */
    @Transactional(readOnly = true)
    public Set<String> effectiveTagSet(UUID workspaceId, UUID callerUserId) {
        if (workspaceId != null) {
            accessControl.requireMember(workspaceId, callerUserId);
        }
        Set<String> tags = new HashSet<>(PosTaggingService.UNIVERSAL_POS_TAGS);
        definitionRepository.findByScope(PosTagScope.GLOBAL)
                .forEach(e -> tags.add(e.getTag()));
        if (workspaceId != null) {
            definitionRepository.findByWorkspaceId(workspaceId)
                    .forEach(e -> tags.add(e.getTag()));
        }
        return tags;
    }

    public void delete(UUID definitionId, UUID callerUserId) {
        PosTagDefinitionEntity entity = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "POS tag definition not found: " + definitionId));

        if (entity.getScope() == PosTagScope.WORKSPACE) {
            requireWorkspaceAdmin(entity.getWorkspaceId(), callerUserId);
        } else {
            if (!entity.getCreatedByUserId().equals(callerUserId.toString())) {
                throw new UnauthorizedException(
                        "Global POS tags can only be deleted by their creator", true);
            }
        }
        definitionRepository.delete(entity);
    }

    private void requireWorkspaceAdmin(UUID workspaceId, UUID callerUserId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workspace not found: " + workspaceId));
        if (workspace.getOwner() != null && callerUserId.equals(workspace.getOwner().getId())) {
            return;
        }
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, callerUserId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Not a member of this workspace", true));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedException(
                    "Only workspace owner or admins can manage POS tags", true);
        }
    }
}
