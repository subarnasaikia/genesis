package com.genesis.workspace.listener;

import com.genesis.common.event.WorkspaceActivityEvent;
import com.genesis.workspace.repository.WorkspaceRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for workspace activity events.
 * Updates the workspace's last modified timestamp.
 */
@Component
public class WorkspaceActivityListener {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceActivityListener.class);
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceActivityListener(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    /**
     * Handle workspace activity event.
     * We use REQUIRES_NEW propagation to ensure the update happens in a separate
     * transaction,
     * or at least commits independently if possible, though mostly we just want to
     * ensure it runs.
     * With async, it's definitely separate. But let's stick to synchronous for now
     * unless performance is an issue,
     * to ensure the UI shows the update immediately.
     * Actually, if we use synchronous, we need to be careful about transaction
     * boundaries.
     * If the main transaction fails, this should probably also roll back?
     * Or if the main transaction commits, this should commit.
     *
     * @param event the event
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void onWorkspaceActivity(WorkspaceActivityEvent event) {
        UUID workspaceId = event.getWorkspaceId();
        logger.debug("Updating last modified timestamp for workspace: {}", workspaceId);
        workspaceRepository.updateLastModified(workspaceId, java.time.Instant.now());
    }
}
