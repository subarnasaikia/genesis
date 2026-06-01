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

        @Test
        @DisplayName("countsByWorkspaceIds - batches total + completed counts per workspace (C-008)")
        void countsByWorkspaceIds_batchesCountsPerWorkspace() {
            // workspace: 3 docs, 1 COMPLETE
            documentRepository.save(createDocument("a.txt", 0, DocumentStatus.UPLOADED));
            documentRepository.save(createDocument("b.txt", 1, DocumentStatus.COMPLETE));
            documentRepository.save(createDocument("c.txt", 2, DocumentStatus.IMPORTED));

            // second workspace: 2 docs, both COMPLETE
            Workspace ws2 = new Workspace();
            ws2.setName("Second Workspace");
            ws2.setAnnotationType(AnnotationType.NER);
            ws2.setStatus(WorkspaceStatus.ACTIVE);
            ws2.setOwner(owner);
            ws2 = workspaceRepository.save(ws2);
            Document d1 = createDocument("d1.txt", 0, DocumentStatus.COMPLETE);
            d1.setWorkspace(ws2);
            Document d2 = createDocument("d2.txt", 1, DocumentStatus.COMPLETE);
            d2.setWorkspace(ws2);
            documentRepository.save(d1);
            documentRepository.save(d2);

            // third workspace: no docs → must NOT appear in the result
            Workspace ws3 = new Workspace();
            ws3.setName("Empty Workspace");
            ws3.setAnnotationType(AnnotationType.POS);
            ws3.setStatus(WorkspaceStatus.DRAFT);
            ws3.setOwner(owner);
            ws3 = workspaceRepository.save(ws3);

            List<DocumentRepository.WorkspaceDocumentCounts> counts =
                    documentRepository.countsByWorkspaceIds(
                            List.of(workspace.getId(), ws2.getId(), ws3.getId()),
                            DocumentStatus.COMPLETE);

            assertThat(counts).hasSize(2); // ws3 has no documents → no row

            var byId = counts.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            DocumentRepository.WorkspaceDocumentCounts::getWorkspaceId, c -> c));

            assertThat(byId.get(workspace.getId()).getTotal()).isEqualTo(3);
            assertThat(byId.get(workspace.getId()).getCompleted()).isEqualTo(1);
            assertThat(byId.get(ws2.getId()).getTotal()).isEqualTo(2);
            assertThat(byId.get(ws2.getId()).getCompleted()).isEqualTo(2);
            assertThat(byId).doesNotContainKey(ws3.getId());
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
