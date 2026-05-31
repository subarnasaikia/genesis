package com.genesis.api.query;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.port.DocumentQueryPort;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composition-root adapter implementing {@link DocumentQueryPort} over
 * {@code genesis-workspace}'s {@code DocumentRepository}. Keeps the cross-module
 * data-access reach inside {@code genesis-api} so annotation modules depend only
 * on the port (ARCHITECTURE_AUDIT A-002/A-005), mirroring {@code RecipientDirectoryAdapter}.
 */
@Component
public class DocumentQueryAdapter implements DocumentQueryPort {

    private final DocumentRepository documentRepository;

    public DocumentQueryAdapter(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID workspaceIdForDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        return document.getWorkspace() != null ? document.getWorkspace().getId() : null;
    }
}
