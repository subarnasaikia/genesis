package com.genesis.workspace.repository;

import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Workspace entity operations.
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    /**
     * Find all workspaces owned by a specific user.
     *
     * @param ownerId the owner's user ID
     * @return list of workspaces owned by the user
     */
    List<Workspace> findByOwnerId(UUID ownerId);

    /**
     * Find all workspaces with a specific status.
     *
     * @param status the workspace status
     * @return list of workspaces with the status
     */
    List<Workspace> findByStatus(WorkspaceStatus status);

    /**
     * Find workspaces by owner and status.
     *
     * @param ownerId the owner's user ID
     * @param status  the workspace status
     * @return list of matching workspaces
     */
    List<Workspace> findByOwnerIdAndStatus(UUID ownerId, WorkspaceStatus status);

    /**
     * Check if a workspace with the given name exists for the owner.
     *
     * @param name    the workspace name
     * @param ownerId the owner's user ID
     * @return true if exists
     */
    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
