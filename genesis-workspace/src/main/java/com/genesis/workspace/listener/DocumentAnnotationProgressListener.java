package com.genesis.workspace.listener;

import com.genesis.common.event.MentionAnnotatedEvent;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.service.DocumentService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Recomputes a document's annotation status and progress when coref reports a
 * mention change ({@link MentionAnnotatedEvent}). This logic used to live in
 * {@code genesis-coref}'s {@code MentionService}, which injected
 * {@code DocumentService} directly; moving it here keeps the workspace's
 * document state machine inside the module that owns the {@code Document}
 * (ARCHITECTURE_AUDIT A-001).
 *
 * <p>Runs AFTER_COMMIT so a progress-update failure cannot roll back the mention
 * write that triggered it — matching the original "log, don't fail the operation"
 * behaviour.
 */
@Component
public class DocumentAnnotationProgressListener {

    private static final Logger logger =
            LoggerFactory.getLogger(DocumentAnnotationProgressListener.class);

    private final DocumentService documentService;

    public DocumentAnnotationProgressListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMentionAnnotated(MentionAnnotatedEvent event) {
        UUID documentId = event.getDocumentId();
        try {
            DocumentResponse doc = documentService.getByIdInternal(documentId);

            // First annotation moves an untouched document into ANNOTATING.
            if (doc.getStatus() == DocumentStatus.UPLOADED
                    || doc.getStatus() == DocumentStatus.IMPORTED) {
                documentService.updateStatusInternal(documentId, DocumentStatus.ANNOTATING);
            }

            // Token indices are null until the document is tokenized (UPLOADED/IMPORTED,
            // or mid-tokenization). A mention can be created before that, so guard the
            // unboxing: with no known token range we leave progress at 0.0 rather than
            // NPE on the arithmetic below.
            Integer startIndex = doc.getTokenStartIndex();
            Integer endIndex = doc.getTokenEndIndex();
            double progress = 0.0;
            if (startIndex != null && endIndex != null) {
                long totalTokens = (long) (endIndex - startIndex + 1);
                if (totalTokens > 0) {
                    progress = (double) event.getMentionTokenCount() / totalTokens;
                    if (progress > 1.0) {
                        progress = 1.0; // cap at 100%
                    }
                }
            }
            documentService.updateProgress(documentId, progress);
        } catch (Exception e) {
            // Log but don't fail — the mention write has already committed.
            logger.error("Failed to update document progress for {}", documentId, e);
        }
    }
}
