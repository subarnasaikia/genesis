package com.genesis.api.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DocumentQueryAdapter} — the composition-root adapter that
 * backs the annotation modules' {@link com.genesis.common.port.DocumentQueryPort}
 * (A-002/A-005). Covers the three branches that service-layer tests mock away:
 * resolved workspace, missing document (throw), and document-without-workspace (null).
 */
@ExtendWith(MockitoExtension.class)
class DocumentQueryAdapterTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentQueryAdapter adapter;

    @Test
    @DisplayName("workspaceIdForDocument - returns the owning workspace id")
    void workspaceIdForDocument_resolvesWorkspace() {
        UUID documentId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(workspaceId);
        Document document = mock(Document.class);
        when(document.getWorkspace()).thenReturn(workspace);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        assertThat(adapter.workspaceIdForDocument(documentId)).isEqualTo(workspaceId);
    }

    @Test
    @DisplayName("workspaceIdForDocument - missing document throws ResourceNotFoundException")
    void workspaceIdForDocument_missingDocument_throws() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.workspaceIdForDocument(documentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(documentId.toString());
    }

    @Test
    @DisplayName("workspaceIdForDocument - document not bound to a workspace returns null")
    void workspaceIdForDocument_unboundDocument_returnsNull() {
        UUID documentId = UUID.randomUUID();
        Document document = mock(Document.class);
        when(document.getWorkspace()).thenReturn(null);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        assertThat(adapter.workspaceIdForDocument(documentId)).isNull();
    }
}
