package com.genesis.workspace.service;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralised authorization checks for workspace-scoped resources.
 *
 * <p>
 * Closes CRITICAL-3 (IDOR) — services that mutate or read workspace state must
 * call {@link #requireMember} or {@link #requireAdmin} before doing anything
 * else.
 *
 * <p>
 * Lives in {@code genesis-workspace} because both {@code WorkspaceService} and
 * {@code DocumentService} depend on it and the only repository it touches is
 * {@code WorkspaceMemberRepository}.
 */
@Component
public class WorkspaceAccessControl {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceAccessControl(WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Throws if {@code callerId} is not a member of {@code workspaceId}.
     *
     * @return the {@link WorkspaceMember} row (caller's role can be inspected)
     */
    @Transactional(readOnly = true)
    public WorkspaceMember requireMember(@NonNull UUID workspaceId, @NonNull UUID callerId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Not a member of this workspace", true));
    }

    /**
     * Throws if {@code callerId} is not an ADMIN of {@code workspaceId}.
     */
    @Transactional(readOnly = true)
    public WorkspaceMember requireAdmin(@NonNull UUID workspaceId, @NonNull UUID callerId) {
        WorkspaceMember member = requireMember(workspaceId, callerId);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedException("Admin role required", true);
        }
        return member;
    }
}
