package com.genesis.importexport.service;

import com.genesis.infra.storage.FileStorageService;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.ProcessingStatus;
import com.genesis.workspace.event.DocumentUploadedEvent;
import com.genesis.workspace.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public AsyncDocumentProcessor(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            ImportService importService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.importService = importService;
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
            processDocument(event);
        } catch (Exception e) {
            log.error("Error processing document {}: {}", event.getDocumentId(), e.getMessage(), e);
            markDocumentFailed(event.getDocumentId(), e.getMessage());
        }
    }

    /**
     * Process a document - download content and tokenize.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocument(DocumentUploadedEvent event) {
        // Update status to PROCESSING
        Document document = documentRepository.findById(event.getDocumentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Document not found: " + event.getDocumentId()));

        document.setProcessingStatus(ProcessingStatus.PROCESSING);
        document.setProcessingError(null);
        documentRepository.save(document);

        try {
            // Download file content from storage
            String content = fileStorageService.downloadAsString(event.getStoredFileUrl());

            // Tokenize the content
            ImportService.ImportResult result = importService.importPlainText(
                    event.getDocumentId(), content);

            // Update document with success status
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            documentRepository.save(document);

            log.info("Document {} tokenized successfully: {} sentences, {} tokens",
                    event.getDocumentId(), result.getSentenceCount(), result.getTokenCount());

        } catch (Exception e) {
            // Mark as failed with error message
            document.setProcessingStatus(ProcessingStatus.FAILED);
            document.setProcessingError(truncateError(e.getMessage()));
            documentRepository.save(document);
            throw e;
        }
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
