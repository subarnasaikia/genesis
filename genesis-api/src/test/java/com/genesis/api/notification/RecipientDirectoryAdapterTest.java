package com.genesis.api.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RecipientDirectoryAdapter} — the composition-root
 * adapter that backs the notification module's {@link
 * com.genesis.notification.port.RecipientDirectory} port (A-004). These cover
 * the translation logic that moved out of the notification module: member-id
 * mapping, name concatenation, and the missing-user fallback.
 */
@ExtendWith(MockitoExtension.class)
class RecipientDirectoryAdapterTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecipientDirectoryAdapter adapter;

    @Test
    @DisplayName("workspaceMemberUserIds - maps each member to its user id")
    void workspaceMemberUserIds_mapsMemberToUserId() {
        UUID workspaceId = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        // Build the member mocks before the outer stub — nesting when() inside
        // when().thenReturn(...) trips Mockito's UnfinishedStubbing check.
        WorkspaceMember member1 = memberWithUserId(u1);
        WorkspaceMember member2 = memberWithUserId(u2);
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId))
                .thenReturn(List.of(member1, member2));

        assertThat(adapter.workspaceMemberUserIds(workspaceId)).containsExactly(u1, u2);
    }

    @Test
    @DisplayName("workspaceMemberUserIds - empty workspace yields empty list")
    void workspaceMemberUserIds_emptyWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of());

        assertThat(adapter.workspaceMemberUserIds(workspaceId)).isEmpty();
    }

    @Test
    @DisplayName("username - present user returns its username")
    void username_present() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("ada");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(adapter.username(userId)).contains("ada");
    }

    @Test
    @DisplayName("username - missing user returns empty (WebSocket routing is skipped)")
    void username_missing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(adapter.username(userId)).isEmpty();
    }

    @Test
    @DisplayName("displayName - present user returns 'First Last'")
    void displayName_present() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getFirstName()).thenReturn("Ada");
        when(user.getLastName()).thenReturn("Lovelace");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(adapter.displayName(userId)).isEqualTo("Ada Lovelace");
    }

    @Test
    @DisplayName("displayName - missing user falls back to 'Unknown User'")
    void displayName_missing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(adapter.displayName(userId)).isEqualTo("Unknown User");
    }

    private WorkspaceMember memberWithUserId(UUID userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getUser()).thenReturn(user);
        return member;
    }
}
