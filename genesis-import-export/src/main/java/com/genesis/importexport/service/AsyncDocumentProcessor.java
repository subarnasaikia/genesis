package com.genesis.importexport.service;

import com.genesis.infra.storage.FileStorageService;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.ProcessingStatus;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.event.DocumentUploadedEvent;
import com.genesis.workspace.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async processor for document operations.
 * Handles background tokenization of uploaded documents.
 */
@Service
public class AsyncDocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncDocumentProcessor.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ImportService importService;
    private final ApplicationEventPublisher eventPublisher;

    public AsyncDocumentProcessor(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            ImportService importService,
            ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.importService = importService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle document uploaded event - triggers async tokenization.
     * Executes after the upload transaction commits.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        log.info("Starting async tokenization for document: {}", event.getDocumentId());

        try {
            String documentName = processDocument(event);

            // Publish tokenization complete event AFTER transaction commits
            // This ensures the notification listener can see the committed data
            eventPublisher.publishEvent(new DocumentTokenizedEvent(
                    this,
                    event.getDocumentId(),
                    event.getWorkspaceId(),
                    documentName));

            log.info("Published DocumentTokenizedEvent for document: {}", event.getDocumentId());

        } catch (Exception e) {
            log.error("Error processing document {}: {}", event.getDocumentId(), e.getMessage(), e);
            markDocumentFailed(event.getDocumentId(), e.getMessage());
        }
    }

    /**
     * Process a document - download content and tokenize.
     * Returns the document name on success for the notification.
     */
    public String processDocument(DocumentUploadedEvent event) {
        String documentName;

        // Phase 1: Update status to PROCESSING
        documentName = updateStatusToProcessing(event.getDocumentId());

        try {
            // Phase 2: Download file content from storage
            String content = fileStorageService.downloadAsString(event.getStoredFileUrl());

            // Phase 3: Tokenize the content (handles its own transactions internally)
            ImportService.ImportResult result = importService.importPlainText(
                    event.getDocumentId(), content);

            log.info("Document {} tokenized successfully: {} sentences, {} tokens",
                    event.getDocumentId(), result.getSentenceCount(), result.getTokenCount());

            // Phase 4: Update status to COMPLETED
            updateStatusToCompleted(event.getDocumentId());

            return documentName;

        } catch (Exception e) {
            // Mark as failed with error message
            markDocumentFailed(event.getDocumentId(), e.getMessage());
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String updateStatusToProcessing(java.util.UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        document.setProcessingStatus(ProcessingStatus.PROCESSING);
        document.setProcessingError(null);
        documentRepository.save(document);
        return document.getName();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusToCompleted(java.util.UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        document.setProcessingStatus(ProcessingStatus.COMPLETED);
        documentRepository.save(document);
    }

    /**
     * Mark document as failed (for use in catch blocks outside transaction).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDocumentFailed(java.util.UUID documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setProcessingStatus(ProcessingStatus.FAILED);
            document.setProcessingError(truncateError(errorMessage));
            documentRepository.save(document);
        });
    }

    private String truncateError(String error) {
        if (error == null) {
            return "Unknown error";
        }
        return error.length() > 900 ? error.substring(0, 900) + "..." : error;
    }
}
