package com.genesis.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceAccessControlTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    private WorkspaceAccessControl accessControl;

    private UUID workspaceId;
    private UUID callerId;

    @BeforeEach
    void setUp() {
        accessControl = new WorkspaceAccessControl(workspaceMemberRepository);
        workspaceId = UUID.randomUUID();
        callerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("requireMember - caller is annotator - returns membership")
    void requireMember_annotator_returnsMembership() {
        WorkspaceMember member = membership(MemberRole.ANNOTATOR);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.of(member));

        WorkspaceMember result = accessControl.requireMember(workspaceId, callerId);

        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("requireMember - caller is admin - returns membership")
    void requireMember_admin_returnsMembership() {
        WorkspaceMember member = membership(MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.of(member));

        WorkspaceMember result = accessControl.requireMember(workspaceId, callerId);

        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("requireMember - caller is outsider - throws 403")
    void requireMember_outsider_throws() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessControl.requireMember(workspaceId, callerId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not a member");
    }

    @Test
    @DisplayName("requireAdmin - caller is admin - returns membership")
    void requireAdmin_admin_returnsMembership() {
        WorkspaceMember member = membership(MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.of(member));

        WorkspaceMember result = accessControl.requireAdmin(workspaceId, callerId);

        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("requireAdmin - caller is annotator - throws 403")
    void requireAdmin_annotator_throws() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.of(membership(MemberRole.ANNOTATOR)));

        assertThatThrownBy(() -> accessControl.requireAdmin(workspaceId, callerId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Admin role required");
    }

    @Test
    @DisplayName("requireAdmin - caller is curator - throws 403")
    void requireAdmin_curator_throws() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.of(membership(MemberRole.CURATOR)));

        assertThatThrownBy(() -> accessControl.requireAdmin(workspaceId, callerId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Admin role required");
    }

    @Test
    @DisplayName("requireAdmin - caller is outsider - throws 403")
    void requireAdmin_outsider_throws() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessControl.requireAdmin(workspaceId, callerId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not a member");
    }

    private WorkspaceMember membership(MemberRole role) {
        WorkspaceMember m = new WorkspaceMember();
        m.setRole(role);
        return m;
    }
}
