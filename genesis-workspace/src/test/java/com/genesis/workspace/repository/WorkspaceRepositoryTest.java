package com.genesis.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.config.WorkspaceTestConfiguration;
import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.Workspace;
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
 * TDD Tests for WorkspaceRepository.
 * RED phase - Tests written before implementation.
 */
@DataJpaTest
@ContextConfiguration(classes = WorkspaceTestConfiguration.class)
class WorkspaceRepositoryTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    private User testOwner;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // Create test owner
        testOwner = new User();
        testOwner.setUsername("owner");
        testOwner.setEmail("owner@example.com");
        testOwner.setPassword("password123");
        testOwner.setFirstName("Test");
        testOwner.setLastName("Owner");
        testOwner.setAuthProvider(AuthProvider.LOCAL);
        testOwner.setEnabled(true);
        testOwner = userRepository.save(testOwner);

        // Create test workspace
        testWorkspace = new Workspace();
        testWorkspace.setName("Test Workspace");
        testWorkspace.setDescription("A test workspace for TDD");
        testWorkspace.setAnnotationType(AnnotationType.COREF);
        testWorkspace.setStatus(WorkspaceStatus.DRAFT);
        testWorkspace.setOwner(testOwner);
        testWorkspace = workspaceRepository.save(testWorkspace);
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("save - creates workspace with generated ID")
        void save_createsWorkspaceWithGeneratedId() {
            Workspace workspace = new Workspace();
            workspace.setName("New Workspace");
            workspace.setDescription("Description");
            workspace.setAnnotationType(AnnotationType.NER);
            workspace.setStatus(WorkspaceStatus.DRAFT);
            workspace.setOwner(testOwner);

            Workspace saved = workspaceRepository.save(workspace);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("New Workspace");
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("findById - existing workspace - returns workspace")
        void findById_existingWorkspace_returnsWorkspace() {
            Optional<Workspace> found = workspaceRepository.findById(testWorkspace.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Test Workspace");
            assertThat(found.get().getOwner().getId()).isEqualTo(testOwner.getId());
        }

        @Test
        @DisplayName("findById - non-existent - returns empty")
        void findById_nonExistent_returnsEmpty() {
            Optional<Workspace> found = workspaceRepository.findById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("delete - removes workspace")
        void delete_removesWorkspace() {
            UUID id = testWorkspace.getId();

            workspaceRepository.delete(testWorkspace);

            assertThat(workspaceRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by Owner")
    class FindByOwner {

        @Test
        @DisplayName("findByOwnerId - returns owner's workspaces")
        void findByOwnerId_returnsOwnersWorkspaces() {
            // Create another workspace for same owner
            Workspace another = new Workspace();
            another.setName("Another Workspace");
            another.setAnnotationType(AnnotationType.POS);
            another.setStatus(WorkspaceStatus.ACTIVE);
            another.setOwner(testOwner);
            workspaceRepository.save(another);

            List<Workspace> workspaces = workspaceRepository.findByOwnerId(testOwner.getId());

            assertThat(workspaces).hasSize(2);
            assertThat(workspaces).extracting(Workspace::getName)
                    .containsExactlyInAnyOrder("Test Workspace", "Another Workspace");
        }

        @Test
        @DisplayName("findByOwnerId - other user - returns empty")
        void findByOwnerId_otherUser_returnsEmpty() {
            // Create different owner
            User other = new User();
            other.setUsername("other");
            other.setEmail("other@example.com");
            other.setPassword("password");
            other.setFirstName("Other");
            other.setLastName("User");
            other.setAuthProvider(AuthProvider.LOCAL);
            other.setEnabled(true);
            other = userRepository.save(other);

            List<Workspace> workspaces = workspaceRepository.findByOwnerId(other.getId());

            assertThat(workspaces).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by Status")
    class FindByStatus {

        @Test
        @DisplayName("findByStatus - returns matching workspaces")
        void findByStatus_returnsMatchingWorkspaces() {
            List<Workspace> drafts = workspaceRepository.findByStatus(WorkspaceStatus.DRAFT);

            assertThat(drafts).hasSize(1);
            assertThat(drafts.get(0).getStatus()).isEqualTo(WorkspaceStatus.DRAFT);
        }

        @Test
        @DisplayName("findByOwnerIdAndStatus - filters by both")
        void findByOwnerIdAndStatus_filtersByBoth() {
            // Create active workspace
            Workspace active = new Workspace();
            active.setName("Active Workspace");
            active.setAnnotationType(AnnotationType.COREF);
            active.setStatus(WorkspaceStatus.ACTIVE);
            active.setOwner(testOwner);
            workspaceRepository.save(active);

            List<Workspace> activesForOwner = workspaceRepository
                    .findByOwnerIdAndStatus(testOwner.getId(), WorkspaceStatus.ACTIVE);

            assertThat(activesForOwner).hasSize(1);
            assertThat(activesForOwner.get(0).getName()).isEqualTo("Active Workspace");
        }
    }

    @Nested
    @DisplayName("Exists Checks")
    class ExistsChecks {

        @Test
        @DisplayName("existsByNameAndOwnerId - existing - returns true")
        void existsByNameAndOwnerId_existing_returnsTrue() {
            boolean exists = workspaceRepository.existsByNameAndOwnerId("Test Workspace", testOwner.getId());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByNameAndOwnerId - different owner - returns false")
        void existsByNameAndOwnerId_differentOwner_returnsFalse() {
            boolean exists = workspaceRepository.existsByNameAndOwnerId("Test Workspace", UUID.randomUUID());

            assertThat(exists).isFalse();
        }
    }
}
