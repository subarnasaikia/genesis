package com.genesis.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import com.genesis.workspace.config.WorkspaceTestConfiguration;
import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
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
 * TDD Tests for DocumentRepository.
 * RED phase - Tests written before implementation.
 */
@DataJpaTest
@ContextConfiguration(classes = WorkspaceTestConfiguration.class)
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // Create owner
        owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@example.com");
        owner.setPassword("password123");
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setAuthProvider(AuthProvider.LOCAL);
        owner.setEnabled(true);
        owner = userRepository.save(owner);

        // Create workspace
        workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspace.setAnnotationType(AnnotationType.COREF);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setOwner(owner);
        workspace = workspaceRepository.save(workspace);
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("save - creates document with generated ID")
        void save_createsDocumentWithGeneratedId() {
            Document doc = createDocument("test.txt", 0, DocumentStatus.UPLOADED);

            Document saved = documentRepository.save(doc);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("test.txt");
            assertThat(saved.getOrderIndex()).isEqualTo(0);
            assertThat(saved.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        }

        @Test
        @DisplayName("delete - removes document")
        void delete_removesDocument() {
            Document doc = documentRepository.save(createDocument("test.txt", 0, DocumentStatus.UPLOADED));
            UUID id = doc.getId();

            documentRepository.delete(doc);

            assertThat(documentRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by Workspace")
    class FindByWorkspace {

        @Test
        @DisplayName("findByWorkspaceId - returns all documents")
        void findByWorkspaceId_returnsAllDocuments() {
            documentRepository.save(createDocument("file1.txt", 0, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file2.txt", 1, DocumentStatus.IMPORTED));
            documentRepository.save(createDocument("file3.txt", 2, DocumentStatus.COMPLETE));

            List<Document> docs = documentRepository.findByWorkspaceId(workspace.getId());

            assertThat(docs).hasSize(3);
        }

        @Test
        @DisplayName("findByWorkspaceIdOrderByOrderIndexAsc - returns ordered documents")
        void findByWorkspaceIdOrderByOrderIndexAsc_returnsOrderedDocuments() {
            documentRepository.save(createDocument("file3.txt", 2, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file1.txt", 0, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file2.txt", 1, DocumentStatus.UPLOADED));

            List<Document> docs = documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(workspace.getId());

            assertThat(docs).hasSize(3);
            assertThat(docs.get(0).getName()).isEqualTo("file1.txt");
            assertThat(docs.get(1).getName()).isEqualTo("file2.txt");
            assertThat(docs.get(2).getName()).isEqualTo("file3.txt");
        }
    }

    @Nested
    @DisplayName("Find by Status")
    class FindByStatus {

        @Test
        @DisplayName("findByWorkspaceIdAndStatus - filters by status")
        void findByWorkspaceIdAndStatus_filtersByStatus() {
            documentRepository.save(createDocument("file1.txt", 0, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file2.txt", 1, DocumentStatus.IMPORTED));
            documentRepository.save(createDocument("file3.txt", 2, DocumentStatus.IMPORTED));

            List<Document> imported = documentRepository
                    .findByWorkspaceIdAndStatus(workspace.getId(), DocumentStatus.IMPORTED);

            assertThat(imported).hasSize(2);
            assertThat(imported).extracting(Document::getName)
                    .containsExactlyInAnyOrder("file2.txt", "file3.txt");
        }
    }

    @Nested
    @DisplayName("Count and Max Order")
    class CountAndMaxOrder {

        @Test
        @DisplayName("countByWorkspaceId - returns document count")
        void countByWorkspaceId_returnsDocumentCount() {
            documentRepository.save(createDocument("file1.txt", 0, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file2.txt", 1, DocumentStatus.UPLOADED));

            long count = documentRepository.countByWorkspaceId(workspace.getId());

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("findMaxOrderIndexByWorkspaceId - returns max order index")
        void findMaxOrderIndexByWorkspaceId_returnsMaxOrderIndex() {
            documentRepository.save(createDocument("file1.txt", 0, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file2.txt", 1, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("file3.txt", 5, DocumentStatus.UPLOADED));

            Optional<Integer> maxOrder = documentRepository.findMaxOrderIndexByWorkspaceId(workspace.getId());

            assertThat(maxOrder).isPresent();
            assertThat(maxOrder.get()).isEqualTo(5);
        }

        @Test
        @DisplayName("findMaxOrderIndexByWorkspaceId - empty workspace - returns empty")
        void findMaxOrderIndexByWorkspaceId_emptyWorkspace_returnsEmpty() {
            Optional<Integer> maxOrder = documentRepository.findMaxOrderIndexByWorkspaceId(workspace.getId());

            assertThat(maxOrder).isEmpty();
        }
    }

    @Nested
    @DisplayName("Token Ranges")
    class TokenRanges {

        @Test
        @DisplayName("documents store token start and end indices")
        void documentsStoreTokenIndices() {
            Document doc = createDocument("file1.txt", 0, DocumentStatus.IMPORTED);
            doc.setTokenStartIndex(0);
            doc.setTokenEndIndex(50);

            Document saved = documentRepository.save(doc);

            assertThat(saved.getTokenStartIndex()).isEqualTo(0);
            assertThat(saved.getTokenEndIndex()).isEqualTo(50);
        }
    }

    private Document createDocument(String name, int orderIndex, DocumentStatus status) {
        Document doc = new Document();
        doc.setName(name);
        doc.setOrderIndex(orderIndex);
        doc.setStatus(status);
        doc.setWorkspace(workspace);
        return doc;
    }
}
