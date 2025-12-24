package com.genesis.editor.service;

import com.genesis.editor.dto.EditorSessionResponse;
import com.genesis.editor.dto.SaveSessionRequest;
import com.genesis.editor.entity.EditorSession;
import com.genesis.editor.repository.EditorSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing editor sessions.
 *
 * <p>Handles saving and restoring user's editor state for a workspace,
 * including last document viewed and scroll position.
 */
@Service
public class EditorService {

    private final EditorSessionRepository editorSessionRepository;

    public EditorService(EditorSessionRepository editorSessionRepository) {
        this.editorSessionRepository = editorSessionRepository;
    }

    /**
     * Get the current session for a user and workspace.
     * Returns empty if no session exists.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     * @return the session if exists
     */
    public Optional<EditorSessionResponse> getSession(@NonNull UUID workspaceId, @NonNull UUID userId) {
        return editorSessionRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(this::mapToResponse);
    }

    /**
     * Open a workspace in the editor.
     * Creates or updates the session with a new lastAccessedAt timestamp.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     * @return the session
     */
    @Transactional
    public EditorSessionResponse openWorkspace(@NonNull UUID workspaceId, @NonNull UUID userId) {
        EditorSession session = editorSessionRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseGet(() -> {
                    EditorSession newSession = new EditorSession();
                    newSession.setWorkspaceId(workspaceId);
                    newSession.setUserId(userId);
                    newSession.setLastDocumentIndex(0);
                    newSession.setScrollPosition(0);
                    return newSession;
                });

        session.setLastAccessedAt(Instant.now());
        EditorSession saved = editorSessionRepository.save(session);
        return mapToResponse(saved);
    }

    /**
     * Save session state.
     *
     * @param request the save request
     * @param userId  the user ID
     * @return the updated session
     */
    @Transactional
    public EditorSessionResponse saveSession(@NonNull SaveSessionRequest request, @NonNull UUID userId) {
        EditorSession session = editorSessionRepository
                .findByWorkspaceIdAndUserId(request.getWorkspaceId(), userId)
                .orElseGet(() -> {
                    EditorSession newSession = new EditorSession();
                    newSession.setWorkspaceId(request.getWorkspaceId());
                    newSession.setUserId(userId);
                    return newSession;
                });

        if (request.getLastDocumentIndex() != null) {
            session.setLastDocumentIndex(request.getLastDocumentIndex());
        }
        if (request.getScrollPosition() != null) {
            session.setScrollPosition(request.getScrollPosition());
        }
        session.setLastAccessedAt(Instant.now());

        EditorSession saved = editorSessionRepository.save(session);
        return mapToResponse(saved);
    }

    /**
     * Close/clear the session for a workspace.
     *
     * @param workspaceId the workspace ID
     * @param userId      the user ID
     */
    @Transactional
    public void closeSession(@NonNull UUID workspaceId, @NonNull UUID userId) {
        editorSessionRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }

    private EditorSessionResponse mapToResponse(EditorSession session) {
        EditorSessionResponse response = new EditorSessionResponse();
        response.setId(session.getId());
        response.setWorkspaceId(session.getWorkspaceId());
        response.setUserId(session.getUserId());
        response.setLastDocumentIndex(session.getLastDocumentIndex());
        response.setScrollPosition(session.getScrollPosition());
        response.setLastAccessedAt(session.getLastAccessedAt());
        return response;
    }
}
