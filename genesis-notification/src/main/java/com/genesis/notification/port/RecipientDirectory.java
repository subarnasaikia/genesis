package com.genesis.notification.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for the notification module: resolves <em>who</em> to notify and
 * <em>how to address them</em> without the module depending on the workspace or
 * user data model (repositories/entities) directly.
 *
 * <p>This is the notification module's own contract, expressed in its own terms.
 * The implementing adapter lives in the composition root ({@code genesis-api}),
 * which owns the cross-module wiring — so the dependency arrow points inward
 * (api → notification), never notification → workspace/user repositories
 * (ARCHITECTURE_AUDIT A-004).
 */
public interface RecipientDirectory {

    /**
     * User ids of every member of a workspace — the fan-out targets for a
     * workspace-scoped notification.
     *
     * @param workspaceId the workspace whose members to resolve
     * @return member user ids (empty if the workspace has no members or is gone)
     */
    List<UUID> workspaceMemberUserIds(UUID workspaceId);

    /**
     * Login username for a user id, used for WebSocket routing
     * ({@code convertAndSendToUser} addresses by username, not id).
     *
     * @param userId the user id
     * @return the username, or empty if no such user exists
     */
    Optional<String> username(UUID userId);

    /**
     * Human-readable display name ("First Last") for a user id, used in
     * notification message bodies.
     *
     * @param userId the user id
     * @return the display name, or {@code "Unknown User"} if the user is missing
     */
    String displayName(UUID userId);
}
