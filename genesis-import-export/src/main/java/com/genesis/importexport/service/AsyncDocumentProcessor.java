package com.genesis.importexport.service;

import com.genesis.infra.storage.FileStorageService;
import com.genesis.workspace.event.DocumentProcessingFailedEvent;
import com.genesis.workspace.event.DocumentProcessingStartedEvent;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.event.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async processor for document operations. Handles background tokenization of
 * uploaded documents.
 *
 * <p>Owns no workspace data: it signals progress through events
 * ({@link DocumentProcessingStartedEvent}, {@link DocumentTokenizedEvent},
 * {@link DocumentProcessingFailedEvent}) and {@code genesis-workspace} updates the
 * {@code Document} status from its own listener. This keeps {@code
 * genesis-import-export} off the workspace repository (ARCHITECTURE_AUDIT A-006).
 */
@Service
public class AsyncDocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncDocumentProcessor.class);

    private final FileStorageService fileStorageService;
    private final ImportService importService;
    private final ApplicationEventPublisher eventPublisher;

    public AsyncDocumentProcessor(
            FileStorageService fileStorageService,
            ImportService importService,
            ApplicationEventPublisher eventPublisher) {
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
            // Workspace moves the document to PROCESSING in response to this. Kept
            // inside the try so a failure here still routes to the FAILED event
            // rather than escaping uncaught and leaving the document in PENDING.
            eventPublisher.publishEvent(new DocumentProcessingStartedEvent(this, event.getDocumentId()));

            processDocument(event);

            // Success: workspace marks COMPLETED and the notification module fires.
            // The document name is carried on the upload event (it is the original
            // filename), so we don't need to read the workspace entity here.
            eventPublisher.publishEvent(new DocumentTokenizedEvent(
                    this,
                    event.getDocumentId(),
                    event.getWorkspaceId(),
                    event.getFileName()));

            log.info("Published DocumentTokenizedEvent for document: {}", event.getDocumentId());

        } catch (Exception e) {
            log.error("Error processing document {}: {}", event.getDocumentId(), e.getMessage(), e);
            eventPublisher.publishEvent(new DocumentProcessingFailedEvent(
                    this, event.getDocumentId(), truncateError(e.getMessage())));
        }
    }

    /**
     * Download the document content and tokenize it. Throws on any failure so the
     * caller can publish {@link DocumentProcessingFailedEvent}.
     */
    private void processDocument(DocumentUploadedEvent event) {
        // Download file content from storage
        String content = fileStorageService.downloadAsString(event.getStoredFileUrl());

        // Tokenize the content (handles its own transactions internally).
        // CoNLL files preserve their existing token grid + coreference annotations;
        // plain text goes through the regular sentence/token segmentation path.
        ImportService.ImportResult result;
        if (isConllFile(event.getFileName(), content)) {
            log.info("Document {} detected as CoNLL-2012, importing with coref preservation",
                    event.getDocumentId());
            try {
                result = importService.importConll2012(
                        event.getDocumentId(), event.getWorkspaceId(), content);
            } catch (java.io.IOException ioe) {
                throw new IllegalStateException("Failed to parse CoNLL file: " + ioe.getMessage(), ioe);
            }
        } else {
            result = importService.importPlainText(event.getDocumentId(), content);
        }

        log.info("Document {} tokenized successfully: {} sentences, {} tokens",
                event.getDocumentId(), result.getSentenceCount(), result.getTokenCount());
    }

    private String truncateError(String error) {
        if (error == null) {
            return "Unknown error";
        }
        return error.length() > 900 ? error.substring(0, 900) + "..." : error;
    }

    /**
     * Detect a CoNLL-2012 file via filename extension or content signature.
     */
    static boolean isConllFile(String fileName, String content) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".conll") || lower.endsWith(".conll2012")) {
                return true;
            }
        }
        if (content != null) {
            // Content sniff: scan past leading whitespace/blank lines
            int i = 0;
            int n = content.length();
            while (i < n && Character.isWhitespace(content.charAt(i))) i++;
            if (i < n && content.startsWith("#begin document", i)) {
                return true;
            }
        }
        return false;
    }
}
