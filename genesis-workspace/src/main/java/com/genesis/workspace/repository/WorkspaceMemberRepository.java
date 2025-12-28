package com.genesis.workspace.repository;

import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for WorkspaceMember entity operations.
 */
@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    /**
     * Find all members of a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of workspace members
     */
    List<WorkspaceMember> findByWorkspaceId(UUID workspaceId);

    /**
     * Find members of a workspace with a specific role.
     *
     * @param workspaceId the workspace ID
     * @param role        the member role
     * @return list of matching members
     */
    List<WorkspaceMember> findByWorkspaceIdAndRole(UUID workspaceId, MemberRole role);

    /**
     * Find all workspace memberships for a user.
     *
     * @param userId the user ID
     * @return list of memberships
     */
    List<WorkspaceMember> findByUserId(UUID userId);

    /**
     * Check if a user is a member of a workspace.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     * @return true if user is a member
     */
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    /**
     * Find a specific membership.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     * @return the membership if found
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    void deleteByWorkspaceId(UUID workspaceId);

    /**
     * Delete a user's membership from a workspace.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     */
    void deleteByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
