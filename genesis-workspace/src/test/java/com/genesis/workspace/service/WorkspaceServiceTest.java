package com.genesis.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.dto.AddMemberRequest;
import com.genesis.workspace.dto.CreateWorkspaceRequest;
import com.genesis.workspace.dto.MemberResponse;
import com.genesis.workspace.dto.UpdateWorkspaceRequest;
import com.genesis.workspace.dto.WorkspaceResponse;
import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.entity.WorkspaceStatus;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import com.genesis.workspace.repository.DocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TDD Tests for WorkspaceService.
 * RED phase - Tests written before implementation.
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

        @Mock
        private WorkspaceRepository workspaceRepository;

        @Mock
        private WorkspaceMemberRepository workspaceMemberRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private DocumentRepository documentRepository;

        private WorkspaceService workspaceService;

        private User testOwner;
        private Workspace testWorkspace;

        @BeforeEach
        void setUp() {
                workspaceService = new WorkspaceService(
                                workspaceRepository, workspaceMemberRepository, userRepository, documentRepository);

                testOwner = createUser("owner", "owner@example.com");
                testOwner.setId(UUID.randomUUID());

                testWorkspace = new Workspace();
                testWorkspace.setId(UUID.randomUUID());
                testWorkspace.setName("Test Workspace");
                testWorkspace.setDescription("Description");
                testWorkspace.setAnnotationType(AnnotationType.COREF);
                testWorkspace.setStatus(WorkspaceStatus.DRAFT);
                testWorkspace.setOwner(testOwner);
                testWorkspace.setCreatedAt(Instant.now());
                testWorkspace.setUpdatedAt(Instant.now());
        }

        private User createUser(String username, String email) {
                User user = new User();
                user.setId(UUID.randomUUID());
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword("password");
                user.setFirstName("Test");
                user.setLastName("User");
                user.setAuthProvider(AuthProvider.LOCAL);
                return user;
        }

        @Nested
        @DisplayName("Create Workspace")
        class CreateWorkspace {

                @Test
                @DisplayName("creates workspace with valid request")
                void createsWorkspaceWithValidRequest() {
                        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                                        "New Workspace", "Description", AnnotationType.COREF);

                        when(userRepository.findById(testOwner.getId())).thenReturn(Optional.of(testOwner));
                        when(workspaceRepository.existsByNameAndOwnerId(request.getName(), testOwner.getId()))
                                        .thenReturn(false);
                        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
                                Workspace ws = invocation.getArgument(0);
                                ws.setId(UUID.randomUUID());
                                ws.setCreatedAt(Instant.now());
                                ws.setUpdatedAt(Instant.now());
                                return ws;
                        });

                        WorkspaceResponse response = workspaceService.create(request, testOwner.getId());

                        assertThat(response).isNotNull();
                        assertThat(response.getName()).isEqualTo("New Workspace");
                        assertThat(response.getAnnotationType()).isEqualTo(AnnotationType.COREF);
                        assertThat(response.getStatus()).isEqualTo(WorkspaceStatus.DRAFT);
                        assertThat(response.getOwnerId()).isEqualTo(testOwner.getId());
                }

                @Test
                @DisplayName("throws when owner not found")
                void throwsWhenOwnerNotFound() {
                        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                                        "New Workspace", "Description", AnnotationType.COREF);
                        UUID invalidOwnerId = UUID.randomUUID();

                        when(userRepository.findById(invalidOwnerId)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> workspaceService.create(request, invalidOwnerId))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("User");
                }

                @Test
                @DisplayName("throws when workspace name already exists for owner")
                void throwsWhenNameExists() {
                        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                                        "Existing Workspace", "Description", AnnotationType.COREF);

                        when(userRepository.findById(testOwner.getId())).thenReturn(Optional.of(testOwner));
                        when(workspaceRepository.existsByNameAndOwnerId(request.getName(), testOwner.getId()))
                                        .thenReturn(true);

                        assertThatThrownBy(() -> workspaceService.create(request, testOwner.getId()))
                                        .isInstanceOf(ValidationException.class)
                                        .hasMessageContaining("already exists");

                        verify(workspaceRepository, never()).save(any());
                }
        }

        @Nested
        @DisplayName("Get Workspace")
        class GetWorkspace {

                @Test
                @DisplayName("returns workspace by ID")
                void returnsWorkspaceById() {
                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));

                        WorkspaceResponse response = workspaceService.getById(testWorkspace.getId());

                        assertThat(response.getId()).isEqualTo(testWorkspace.getId());
                        assertThat(response.getName()).isEqualTo("Test Workspace");
                }

                @Test
                @DisplayName("throws when workspace not found")
                void throwsWhenNotFound() {
                        UUID invalidId = UUID.randomUUID();
                        when(workspaceRepository.findById(invalidId)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> workspaceService.getById(invalidId))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Workspace");
                }
        }

        @Nested
        @DisplayName("List Workspaces")
        class ListWorkspaces {

                @Test
                @DisplayName("returns workspaces for member")
                void returnsWorkspacesForMember() {
                        Workspace ws1 = testWorkspace;
                        Workspace ws2 = new Workspace();
                        ws2.setId(UUID.randomUUID());
                        ws2.setName("Second Workspace");
                        ws2.setAnnotationType(AnnotationType.NER);
                        ws2.setStatus(WorkspaceStatus.ACTIVE);
                        ws2.setOwner(testOwner);
                        ws2.setCreatedAt(Instant.now());
                        ws2.setUpdatedAt(Instant.now());

                        when(workspaceRepository.findByMemberUserId(testOwner.getId()))
                                        .thenReturn(List.of(ws1, ws2));

                        List<WorkspaceResponse> responses = workspaceService.getAllForUser(testOwner.getId());

                        assertThat(responses).hasSize(2);
                        assertThat(responses).extracting(WorkspaceResponse::getName)
                                        .containsExactlyInAnyOrder("Test Workspace", "Second Workspace");
                }
        }

        @Nested
        @DisplayName("Add Member")
        class AddMemberTests {

                @Test
                @DisplayName("adds member to workspace")
                void addsMemberToWorkspace() {
                        User newMember = createUser("member", "member@example.com");
                        AddMemberRequest request = new AddMemberRequest("member@example.com", MemberRole.ANNOTATOR);

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(userRepository.findByEmail(newMember.getEmail()))
                                        .thenReturn(Optional.of(newMember));
                        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                                        testWorkspace.getId(), newMember.getId())).thenReturn(false);
                        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        workspaceService.addMember(testWorkspace.getId(), request);

                        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
                        verify(workspaceMemberRepository).save(captor.capture());
                        WorkspaceMember saved = captor.getValue();

                        assertThat(saved.getWorkspace().getId()).isEqualTo(testWorkspace.getId());
                        assertThat(saved.getUser().getId()).isEqualTo(newMember.getId());
                        assertThat(saved.getRole()).isEqualTo(MemberRole.ANNOTATOR);
                }

                @Test
                @DisplayName("throws when user already a member")
                void throwsWhenAlreadyMember() {
                        User existingMember = createUser("existing", "existing@example.com");
                        AddMemberRequest request = new AddMemberRequest("existing@example.com", MemberRole.ANNOTATOR);

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(userRepository.findByEmail(existingMember.getEmail()))
                                        .thenReturn(Optional.of(existingMember));
                        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                                        testWorkspace.getId(), existingMember.getId())).thenReturn(true);

                        assertThatThrownBy(() -> workspaceService.addMember(testWorkspace.getId(), request))
                                        .isInstanceOf(ValidationException.class)
                                        .hasMessageContaining("already a member");

                        verify(workspaceMemberRepository, never()).save(any());
                }
        }

        @Nested
        @DisplayName("Remove Member")
        class RemoveMemberTests {

                @Test
                @DisplayName("removes member from workspace")
                void removesMemberFromWorkspace() {
                        UUID memberUserId = UUID.randomUUID();

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                                        testWorkspace.getId(), memberUserId)).thenReturn(true);

                        workspaceService.removeMember(testWorkspace.getId(), memberUserId);

                        verify(workspaceMemberRepository).deleteByWorkspaceIdAndUserId(
                                        testWorkspace.getId(), memberUserId);
                }

                @Test
                @DisplayName("throws when member not found")
                void throwsWhenMemberNotFound() {
                        UUID nonMemberUserId = UUID.randomUUID();

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                                        testWorkspace.getId(), nonMemberUserId)).thenReturn(false);

                        assertThatThrownBy(() -> workspaceService.removeMember(testWorkspace.getId(), nonMemberUserId))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Member");
                }
        }

        @Nested
        @DisplayName("Update Status")
        class UpdateStatus {

                @Test
                @DisplayName("updates workspace status")
                void updatesWorkspaceStatus() {
                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(workspaceRepository.save(any(Workspace.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        WorkspaceResponse response = workspaceService.updateStatus(
                                        testWorkspace.getId(), WorkspaceStatus.ACTIVE);

                        assertThat(response.getStatus()).isEqualTo(WorkspaceStatus.ACTIVE);
                        verify(workspaceRepository).save(any(Workspace.class));
                }
        }

        @Nested
        @DisplayName("Update Workspace")
        class UpdateWorkspace {

                @Test
                @DisplayName("updates workspace details")
                void updatesWorkspaceDetails() {
                        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest("Updated Name",
                                        "Updated Description");

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(workspaceRepository.existsByNameAndOwnerId(request.getName(), testOwner.getId()))
                                        .thenReturn(false);
                        when(workspaceRepository.save(any(Workspace.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        WorkspaceResponse response = workspaceService.update(testWorkspace.getId(), request);

                        assertThat(response.getName()).isEqualTo("Updated Name");
                        assertThat(response.getDescription()).isEqualTo("Updated Description");
                }
        }

        @Nested
        @DisplayName("Get Members")
        class GetMembers {

                @Test
                @DisplayName("returns workspace members")
                void returnsWorkspaceMembers() {
                        WorkspaceMember member = new WorkspaceMember();
                        member.setWorkspace(testWorkspace);
                        member.setUser(testOwner);
                        member.setRole(MemberRole.ADMIN);

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(workspaceMemberRepository.findByWorkspaceId(testWorkspace.getId()))
                                        .thenReturn(List.of(member));

                        List<MemberResponse> responses = workspaceService.getMembers(testWorkspace.getId());

                        assertThat(responses).hasSize(1);
                        assertThat(responses.get(0).getUserId()).isEqualTo(testOwner.getId());
                        assertThat(responses.get(0).getRole()).isEqualTo(MemberRole.ADMIN);
                }
        }
}
