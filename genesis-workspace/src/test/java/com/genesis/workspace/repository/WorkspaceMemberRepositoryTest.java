package com.genesis.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.config.WorkspaceTestConfiguration;
import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.entity.WorkspaceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * TDD Tests for WorkspaceMemberRepository.
 * RED phase - Tests written before implementation.
 */
@DataJpaTest
@ContextConfiguration(classes = WorkspaceTestConfiguration.class)
class WorkspaceMemberRepositoryTest {

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User member1;
    private User member2;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        owner = createUser("owner", "owner@example.com");
        member1 = createUser("member1", "member1@example.com");
        member2 = createUser("member2", "member2@example.com");

        // Create workspace
        workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspace.setAnnotationType(AnnotationType.COREF);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setOwner(owner);
        workspace = workspaceRepository.save(workspace);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("save - creates member with generated ID")
        void save_createsMemberWithGeneratedId() {
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(workspace);
            member.setUser(member1);
            member.setRole(MemberRole.ANNOTATOR);

            WorkspaceMember saved = workspaceMemberRepository.save(member);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getRole()).isEqualTo(MemberRole.ANNOTATOR);
            assertThat(saved.getWorkspace().getId()).isEqualTo(workspace.getId());
            assertThat(saved.getUser().getId()).isEqualTo(member1.getId());
        }

        @Test
        @DisplayName("delete - removes member")
        void delete_removesMember() {
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(workspace);
            member.setUser(member1);
            member.setRole(MemberRole.ANNOTATOR);
            member = workspaceMemberRepository.save(member);
            UUID id = member.getId();

            workspaceMemberRepository.delete(member);

            assertThat(workspaceMemberRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by Workspace")
    class FindByWorkspace {

        @Test
        @DisplayName("findByWorkspaceId - returns all members")
        void findByWorkspaceId_returnsAllMembers() {
            // Add members
            addMember(workspace, member1, MemberRole.ANNOTATOR);
            addMember(workspace, member2, MemberRole.CURATOR);

            List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspace.getId());

            assertThat(members).hasSize(2);
            assertThat(members).extracting(m -> m.getUser().getUsername())
                    .containsExactlyInAnyOrder("member1", "member2");
        }

        @Test
        @DisplayName("findByWorkspaceIdAndRole - filters by role")
        void findByWorkspaceIdAndRole_filtersByRole() {
            addMember(workspace, member1, MemberRole.ANNOTATOR);
            addMember(workspace, member2, MemberRole.CURATOR);

            List<WorkspaceMember> annotators = workspaceMemberRepository
                    .findByWorkspaceIdAndRole(workspace.getId(), MemberRole.ANNOTATOR);

            assertThat(annotators).hasSize(1);
            assertThat(annotators.get(0).getUser().getUsername()).isEqualTo("member1");
        }
    }

    @Nested
    @DisplayName("Find by User")
    class FindByUser {

        @Test
        @DisplayName("findByUserId - returns user's memberships")
        void findByUserId_returnsUsersMemberships() {
            // Create another workspace
            Workspace workspace2 = new Workspace();
            workspace2.setName("Second Workspace");
            workspace2.setAnnotationType(AnnotationType.NER);
            workspace2.setStatus(WorkspaceStatus.ACTIVE);
            workspace2.setOwner(owner);
            workspace2 = workspaceRepository.save(workspace2);

            addMember(workspace, member1, MemberRole.ANNOTATOR);
            addMember(workspace2, member1, MemberRole.CURATOR);

            List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(member1.getId());

            assertThat(memberships).hasSize(2);
            assertThat(memberships).extracting(m -> m.getWorkspace().getName())
                    .containsExactlyInAnyOrder("Test Workspace", "Second Workspace");
        }
    }

    @Nested
    @DisplayName("Exists and Find One")
    class ExistsAndFindOne {

        @Test
        @DisplayName("existsByWorkspaceIdAndUserId - existing - returns true")
        void existsByWorkspaceIdAndUserId_existing_returnsTrue() {
            addMember(workspace, member1, MemberRole.ANNOTATOR);

            boolean exists = workspaceMemberRepository
                    .existsByWorkspaceIdAndUserId(workspace.getId(), member1.getId());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByWorkspaceIdAndUserId - not existing - returns false")
        void existsByWorkspaceIdAndUserId_notExisting_returnsFalse() {
            boolean exists = workspaceMemberRepository
                    .existsByWorkspaceIdAndUserId(workspace.getId(), member1.getId());

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("findByWorkspaceIdAndUserId - returns member")
        void findByWorkspaceIdAndUserId_returnsMember() {
            addMember(workspace, member1, MemberRole.ANNOTATOR);

            Optional<WorkspaceMember> found = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), member1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getRole()).isEqualTo(MemberRole.ANNOTATOR);
        }
    }

    private void addMember(Workspace ws, User user, MemberRole role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(user);
        member.setRole(role);
        workspaceMemberRepository.save(member);
    }
}
