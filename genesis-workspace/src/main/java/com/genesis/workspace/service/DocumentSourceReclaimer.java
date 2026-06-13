package com.genesis.workspace.service;

import com.genesis.infra.storage.StoredFile;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detaches a document's raw source file so it can be physically reclaimed.
 *
 * <p>
 * This lives in its own bean (and its own {@code REQUIRES_NEW} transaction) on
 * purpose: the FK clear must <b>commit before</b> the {@code stored_files} row is
 * deleted, otherwise the still-referencing {@code documents.stored_file_id}
 * violates the foreign key. {@link com.genesis.workspace.listener.DocumentSourceRetentionListener}
 * calls this, then performs the physical delete in a separate transaction.
 */
@Service
public class DocumentSourceReclaimer {

    private final DocumentRepository documentRepository;

    public DocumentSourceReclaimer(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Clear a document's link to its stored source file.
     *
     * @param documentId the document whose source should be detached
     * @return the detached stored-file id, or empty if the document has no source
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> detachSource(UUID documentId) {
        return documentRepository.findById(documentId).map(document -> {
            StoredFile source = document.getStoredFile();
            if (source == null) {
                return null;
            }
            UUID storedFileId = source.getId();
            document.setStoredFile(null);
            documentRepository.save(document);
            return storedFileId;
        });
    }
}
