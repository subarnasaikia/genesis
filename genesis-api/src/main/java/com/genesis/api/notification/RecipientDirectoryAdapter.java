package com.genesis.api.notification;

import com.genesis.notification.port.RecipientDirectory;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composition-root adapter for {@link RecipientDirectory}. Bridges the
 * notification module's outbound port to the workspace and user repositories.
 *
 * <p>Living in {@code genesis-api} keeps the cross-module data access in the
 * wiring layer (which already depends on every module), so {@code
 * genesis-notification} never imports a workspace/user repository itself
 * (ARCHITECTURE_AUDIT A-004) — the same pattern used for {@code
 * UserDetailsServiceImpl} (A-007).
 */
@Component
public class RecipientDirectoryAdapter implements RecipientDirectory {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public RecipientDirectoryAdapter(
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> workspaceMemberUserIds(UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                .map(member -> member.getUser().getId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> username(UUID userId) {
        return userRepository.findById(userId).map(u -> u.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public String displayName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown User");
    }
}
