package com.genesis.workspace.listener;

import com.genesis.workspace.entity.ProcessingStatus;
import com.genesis.workspace.event.DocumentProcessingFailedEvent;
import com.genesis.workspace.event.DocumentProcessingStartedEvent;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the {@code Document.processingStatus} lifecycle in response to
 * tokenization events published by {@code genesis-import-export}. This keeps the
 * status writes inside the module that owns the {@code Document} entity, so
 * import-export no longer reaches into {@code DocumentRepository}
 * (ARCHITECTURE_AUDIT A-006).
 *
 * <p>Each handler runs in its own transaction ({@code REQUIRES_NEW}) because the
 * events are published from an async, non-transactional context — mirroring the
 * independent per-status commits the import-export processor used to perform.
 */
@Component
public class DocumentProcessingStatusListener {

    private static final Logger logger =
            LoggerFactory.getLogger(DocumentProcessingStatusListener.class);

    private final DocumentRepository documentRepository;

    public DocumentProcessingStatusListener(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProcessingStarted(DocumentProcessingStartedEvent event) {
        documentRepository.findById(event.getDocumentId()).ifPresent(document -> {
            document.setProcessingStatus(ProcessingStatus.PROCESSING);
            document.setProcessingError(null);
            documentRepository.save(document);
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTokenized(DocumentTokenizedEvent event) {
        documentRepository.findById(event.getDocumentId()).ifPresent(document -> {
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            documentRepository.save(document);
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProcessingFailed(DocumentProcessingFailedEvent event) {
        UUID documentId = event.getDocumentId();
        documentRepository.findById(documentId).ifPresentOrElse(document -> {
            document.setProcessingStatus(ProcessingStatus.FAILED);
            document.setProcessingError(event.getErrorMessage());
            documentRepository.save(document);
        }, () -> logger.warn("Processing failed for missing document {}", documentId));
    }
}
