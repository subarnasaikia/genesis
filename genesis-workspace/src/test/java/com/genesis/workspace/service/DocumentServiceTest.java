package com.genesis.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.infra.storage.FileStorageService;
import com.genesis.infra.storage.StoredFile;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceStatus;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

/**
 * TDD Tests for DocumentService.
 * RED phase - Tests written before implementation.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private WorkspaceRepository workspaceRepository;

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        private DocumentService documentService;

        private Workspace testWorkspace;
        private Document testDocument;

        @BeforeEach
        void setUp() {
                documentService = new DocumentService(
                                documentRepository, workspaceRepository, fileStorageService, eventPublisher);

                User owner = new User();
                owner.setId(UUID.randomUUID());
                owner.setUsername("owner");
                owner.setEmail("owner@example.com");
                owner.setPassword("password");
                owner.setFirstName("Test");
                owner.setLastName("Owner");
                owner.setAuthProvider(AuthProvider.LOCAL);

                testWorkspace = new Workspace();
                testWorkspace.setId(UUID.randomUUID());
                testWorkspace.setName("Test Workspace");
                testWorkspace.setAnnotationType(AnnotationType.COREF);
                testWorkspace.setStatus(WorkspaceStatus.ACTIVE);
                testWorkspace.setOwner(owner);

                testDocument = new Document();
                testDocument.setId(UUID.randomUUID());
                testDocument.setName("test.txt");
                testDocument.setOrderIndex(0);
                testDocument.setStatus(DocumentStatus.UPLOADED);
                testDocument.setWorkspace(testWorkspace);
                testDocument.setCreatedAt(Instant.now());
                testDocument.setUpdatedAt(Instant.now());
        }

        @Nested
        @DisplayName("Upload Document")
        class UploadDocument {

                @Test
                @DisplayName("uploads file and creates document with next orderIndex")
                void uploadsFileAndCreatesDocument() {
                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "newfile.txt", "text/plain", "content".getBytes());

                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(UUID.randomUUID());
                        storedFile.setUrl("https://cloudinary.com/newfile.txt");
                        storedFile.setOriginalFilename("newfile.txt");

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(documentRepository.findMaxOrderIndexByWorkspaceId(testWorkspace.getId()))
                                        .thenReturn(Optional.of(2)); // Existing docs have orderIndex 0, 1, 2
                        when(fileStorageService.store(eq(file), any(String.class)))
                                        .thenReturn(storedFile);
                        when(documentRepository.save(any(Document.class)))
                                        .thenAnswer(inv -> {
                                                Document doc = inv.getArgument(0);
                                                doc.setId(UUID.randomUUID());
                                                doc.setCreatedAt(Instant.now());
                                                doc.setUpdatedAt(Instant.now());
                                                return doc;
                                        });

                        DocumentResponse response = documentService.upload(testWorkspace.getId(), file,
                                        UUID.randomUUID());

                        assertThat(response.getName()).isEqualTo("newfile.txt");
                        assertThat(response.getOrderIndex()).isEqualTo(3); // Next after 2
                        assertThat(response.getStatus()).isEqualTo(DocumentStatus.UPLOADED);

                        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
                        verify(documentRepository).save(captor.capture());
                        assertThat(captor.getValue().getOrderIndex()).isEqualTo(3);
                }

                @Test
                @DisplayName("sets orderIndex to 0 for empty workspace")
                void setsOrderIndexToZeroForEmptyWorkspace() {
                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "first.txt", "text/plain", "content".getBytes());

                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(UUID.randomUUID());
                        storedFile.setUrl("https://cloudinary.com/first.txt");
                        storedFile.setOriginalFilename("first.txt");

                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(documentRepository.findMaxOrderIndexByWorkspaceId(testWorkspace.getId()))
                                        .thenReturn(Optional.empty()); // No existing docs
                        when(fileStorageService.store(eq(file), any(String.class)))
                                        .thenReturn(storedFile);
                        when(documentRepository.save(any(Document.class)))
                                        .thenAnswer(inv -> {
                                                Document doc = inv.getArgument(0);
                                                doc.setId(UUID.randomUUID());
                                                doc.setCreatedAt(Instant.now());
                                                doc.setUpdatedAt(Instant.now());
                                                return doc;
                                        });

                        DocumentResponse response = documentService.upload(testWorkspace.getId(), file,
                                        UUID.randomUUID());

                        assertThat(response.getOrderIndex()).isEqualTo(0);
                }

                @Test
                @DisplayName("throws when workspace not found")
                void throwsWhenWorkspaceNotFound() {
                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "test.txt", "text/plain", "content".getBytes());
                        UUID invalidId = UUID.randomUUID();

                        when(workspaceRepository.findById(invalidId)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> documentService.upload(invalidId, file, UUID.randomUUID()))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Workspace");
                }
        }

        @Nested
        @DisplayName("Get Documents")
        class GetDocuments {

                @Test
                @DisplayName("returns documents ordered by orderIndex")
                void returnsDocumentsOrderedByOrderIndex() {
                        Document doc1 = createDocument("file1.txt", 0);
                        Document doc2 = createDocument("file2.txt", 1);
                        Document doc3 = createDocument("file3.txt", 2);

                        when(workspaceRepository.existsById(testWorkspace.getId())).thenReturn(true);
                        when(documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(testWorkspace.getId()))
                                        .thenReturn(List.of(doc1, doc2, doc3));

                        List<DocumentResponse> responses = documentService.getByWorkspaceId(testWorkspace.getId());

                        assertThat(responses).hasSize(3);
                        assertThat(responses.get(0).getName()).isEqualTo("file1.txt");
                        assertThat(responses.get(1).getName()).isEqualTo("file2.txt");
                        assertThat(responses.get(2).getName()).isEqualTo("file3.txt");
                }
        }

        @Nested
        @DisplayName("Update Status")
        class UpdateDocumentStatus {

                @Test
                @DisplayName("updates document status")
                void updatesDocumentStatus() {
                        when(documentRepository.findById(testDocument.getId()))
                                        .thenReturn(Optional.of(testDocument));
                        when(documentRepository.save(any(Document.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        DocumentResponse response = documentService.updateStatus(
                                        testDocument.getId(), DocumentStatus.IMPORTED);

                        assertThat(response.getStatus()).isEqualTo(DocumentStatus.IMPORTED);
                        verify(documentRepository).save(any(Document.class));
                }
        }

        @Nested
        @DisplayName("Delete Document")
        class DeleteDocument {

                @Test
                @DisplayName("deletes document and storedFile")
                void deletesDocumentAndStoredFile() {
                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(UUID.randomUUID());
                        testDocument.setStoredFile(storedFile);

                        when(documentRepository.findById(testDocument.getId()))
                                        .thenReturn(Optional.of(testDocument));

                        documentService.delete(testDocument.getId(), UUID.randomUUID());

                        verify(fileStorageService).delete(storedFile.getId());
                        verify(documentRepository).delete(testDocument);
                }

                @Test
                @DisplayName("throws when document not found")
                void throwsWhenNotFound() {
                        UUID invalidId = UUID.randomUUID();
                        when(documentRepository.findById(invalidId)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> documentService.delete(invalidId, UUID.randomUUID()))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Document");
                }
        }

        private Document createDocument(String name, int orderIndex) {
                Document doc = new Document();
                doc.setId(UUID.randomUUID());
                doc.setName(name);
                doc.setOrderIndex(orderIndex);
                doc.setStatus(DocumentStatus.UPLOADED);
                doc.setWorkspace(testWorkspace);
                doc.setCreatedAt(Instant.now());
                doc.setUpdatedAt(Instant.now());
                return doc;
        }
}
