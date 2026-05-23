package com.genesis.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.infra.storage.FileStorageService;
import com.genesis.infra.storage.StoredFile;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.AnnotationType;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.entity.WorkspaceStatus;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
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

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private WorkspaceRepository workspaceRepository;

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private WorkspaceMemberRepository workspaceMemberRepository;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        private WorkspaceAccessControl accessControl;
        private DocumentService documentService;

        private Workspace testWorkspace;
        private Document testDocument;
        private UUID callerId;

        @BeforeEach
        void setUp() {
                accessControl = new WorkspaceAccessControl(workspaceMemberRepository);
                documentService = new DocumentService(
                                documentRepository, workspaceRepository, fileStorageService, accessControl,
                                eventPublisher);

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

                callerId = UUID.randomUUID();
        }

        private void stubMember(MemberRole role) {
                WorkspaceMember m = new WorkspaceMember();
                m.setWorkspace(testWorkspace);
                m.setRole(role);
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(testWorkspace.getId(), callerId))
                                .thenReturn(Optional.of(m));
        }

        private void stubOutsider() {
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(testWorkspace.getId(), callerId))
                                .thenReturn(Optional.empty());
        }

        @Nested
        @DisplayName("Upload Document")
        class UploadDocument {

                @Test
                @DisplayName("member uploads file and creates document with next orderIndex")
                void uploadsFileAndCreatesDocument() {
                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "newfile.txt", "text/plain", "content".getBytes());

                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(UUID.randomUUID());
                        storedFile.setUrl("https://cloudinary.com/newfile.txt");
                        storedFile.setOriginalFilename("newfile.txt");

                        stubMember(MemberRole.ANNOTATOR);
                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(documentRepository.findMaxOrderIndexByWorkspaceId(testWorkspace.getId()))
                                        .thenReturn(Optional.of(2));
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

                        DocumentResponse response = documentService.upload(testWorkspace.getId(), file, callerId);

                        assertThat(response.getName()).isEqualTo("newfile.txt");
                        assertThat(response.getOrderIndex()).isEqualTo(3);
                        assertThat(response.getStatus()).isEqualTo(DocumentStatus.UPLOADED);

                        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
                        verify(documentRepository).save(captor.capture());
                        assertThat(captor.getValue().getOrderIndex()).isEqualTo(3);
                }

                @Test
                @DisplayName("outsider cannot upload - throws 403")
                void outsiderCannotUpload() {
                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "newfile.txt", "text/plain", "content".getBytes());

                        stubOutsider();

                        assertThatThrownBy(() -> documentService.upload(testWorkspace.getId(), file, callerId))
                                        .isInstanceOf(UnauthorizedException.class);

                        verify(documentRepository, never()).save(any());
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

                        stubMember(MemberRole.ADMIN);
                        when(workspaceRepository.findById(testWorkspace.getId()))
                                        .thenReturn(Optional.of(testWorkspace));
                        when(documentRepository.findMaxOrderIndexByWorkspaceId(testWorkspace.getId()))
                                        .thenReturn(Optional.empty());
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

                        DocumentResponse response = documentService.upload(testWorkspace.getId(), file, callerId);

                        assertThat(response.getOrderIndex()).isEqualTo(0);
                }
        }

        @Nested
        @DisplayName("Get Documents")
        class GetDocuments {

                @Test
                @DisplayName("member returns documents ordered by orderIndex")
                void returnsDocumentsOrderedByOrderIndex() {
                        Document doc1 = createDocument("file1.txt", 0);
                        Document doc2 = createDocument("file2.txt", 1);
                        Document doc3 = createDocument("file3.txt", 2);

                        stubMember(MemberRole.ANNOTATOR);
                        when(workspaceRepository.existsById(testWorkspace.getId())).thenReturn(true);
                        when(documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(testWorkspace.getId()))
                                        .thenReturn(List.of(doc1, doc2, doc3));

                        List<DocumentResponse> responses = documentService.getByWorkspaceId(testWorkspace.getId(),
                                        callerId);

                        assertThat(responses).hasSize(3);
                        assertThat(responses.get(0).getName()).isEqualTo("file1.txt");
                        assertThat(responses.get(1).getName()).isEqualTo("file2.txt");
                        assertThat(responses.get(2).getName()).isEqualTo("file3.txt");
                }

                @Test
                @DisplayName("outsider cannot list documents - throws 403")
                void outsiderCannotList() {
                        stubOutsider();

                        assertThatThrownBy(() -> documentService.getByWorkspaceId(testWorkspace.getId(), callerId))
                                        .isInstanceOf(UnauthorizedException.class);
                }
        }

        @Nested
        @DisplayName("Update Status")
        class UpdateDocumentStatus {

                @Test
                @DisplayName("member updates document status")
                void updatesDocumentStatus() {
                        stubMember(MemberRole.ANNOTATOR);
                        when(documentRepository.findById(testDocument.getId()))
                                        .thenReturn(Optional.of(testDocument));
                        when(documentRepository.save(any(Document.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        DocumentResponse response = documentService.updateStatus(
                                        testDocument.getId(), DocumentStatus.IMPORTED, callerId);

                        assertThat(response.getStatus()).isEqualTo(DocumentStatus.IMPORTED);
                        verify(documentRepository).save(any(Document.class));
                }

                @Test
                @DisplayName("outsider cannot update status - throws 403")
                void outsiderCannotUpdateStatus() {
                        stubOutsider();
                        when(documentRepository.findById(testDocument.getId()))
                                        .thenReturn(Optional.of(testDocument));

                        assertThatThrownBy(() -> documentService.updateStatus(
                                        testDocument.getId(), DocumentStatus.COMPLETE, callerId))
                                        .isInstanceOf(UnauthorizedException.class);

                        verify(documentRepository, never()).save(any());
                }
        }

        @Nested
        @DisplayName("Delete Document")
        class DeleteDocument {

                @Test
                @DisplayName("admin deletes document and storedFile")
                void deletesDocumentAndStoredFile() {
                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(UUID.randomUUID());
                        testDocument.setStoredFile(storedFile);

                        stubMember(MemberRole.ADMIN);
                        when(documentRepository.findById(testDocument.getId()))
                                        .thenReturn(Optional.of(testDocument));

                        documentService.delete(testDocument.getId(), callerId);

                        verify(fileStorageService).delete(storedFile.getId());
                        verify(documentRepository).delete(testDocument);
                }

                @Test
                @DisplayName("annotator cannot delete - throws 403")
                void annotatorCannotDelete() {
                        stubMember(MemberRole.ANNOTATOR);
                        when(documentRepository.findById(testDocument.getId()))
                                        .thenReturn(Optional.of(testDocument));

                        assertThatThrownBy(() -> documentService.delete(testDocument.getId(), callerId))
                                        .isInstanceOf(UnauthorizedException.class);

                        verify(documentRepository, never()).delete(any(Document.class));
                }

                @Test
                @DisplayName("throws when document not found")
                void throwsWhenNotFound() {
                        UUID invalidId = UUID.randomUUID();
                        when(documentRepository.findById(invalidId)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> documentService.delete(invalidId, callerId))
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
