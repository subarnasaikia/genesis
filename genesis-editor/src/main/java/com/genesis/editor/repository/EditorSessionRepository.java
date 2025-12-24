package com.genesis.editor.repository;

import com.genesis.editor.entity.EditorSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for EditorSession entity.
 */
@Repository
public interface EditorSessionRepository extends JpaRepository<EditorSession, UUID> {

    /**
     * Find session by workspace and user.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     * @return the session if exists
     */
    Optional<EditorSession> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    /**
     * Check if a session exists.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     * @return true if exists
     */
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    /**
     * Delete session by workspace and user.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     */
    void deleteByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
