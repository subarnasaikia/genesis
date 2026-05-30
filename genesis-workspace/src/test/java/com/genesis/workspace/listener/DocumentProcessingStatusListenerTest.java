package com.genesis.workspace.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.ProcessingStatus;
import com.genesis.workspace.event.DocumentProcessingFailedEvent;
import com.genesis.workspace.event.DocumentProcessingStartedEvent;
import com.genesis.workspace.event.DocumentTokenizedEvent;
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
 * Unit tests for {@link DocumentProcessingStatusListener}, which owns the
 * {@code Document.processingStatus} lifecycle in response to tokenization events
 * published by genesis-import-export (A-006).
 */
@ExtendWith(MockitoExtension.class)
class DocumentProcessingStatusListenerTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentProcessingStatusListener listener;

    @Test
    @DisplayName("started - moves the document to PROCESSING and clears any prior error")
    void onProcessingStarted_setsProcessing() {
        UUID id = UUID.randomUUID();
        Document doc = new Document();
        doc.setProcessingStatus(ProcessingStatus.PENDING);
        doc.setProcessingError("stale error");
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        listener.onProcessingStarted(new DocumentProcessingStartedEvent(this, id));

        assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSING);
        assertThat(doc.getProcessingError()).isNull();
        verify(documentRepository).save(doc);
    }

    @Test
    @DisplayName("tokenized - moves the document to COMPLETED")
    void onTokenized_setsCompleted() {
        UUID id = UUID.randomUUID();
        Document doc = new Document();
        doc.setProcessingStatus(ProcessingStatus.PROCESSING);
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        listener.onTokenized(new DocumentTokenizedEvent(this, id, UUID.randomUUID(), "notes.txt"));

        assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        verify(documentRepository).save(doc);
    }

    @Test
    @DisplayName("failed - moves the document to FAILED and records the error")
    void onProcessingFailed_setsFailedWithError() {
        UUID id = UUID.randomUUID();
        Document doc = new Document();
        doc.setProcessingStatus(ProcessingStatus.PROCESSING);
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        listener.onProcessingFailed(new DocumentProcessingFailedEvent(this, id, "boom"));

        assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(doc.getProcessingError()).isEqualTo("boom");
        verify(documentRepository).save(doc);
    }

    @Test
    @DisplayName("failed - missing document is a no-op (no save, no throw)")
    void onProcessingFailed_missingDocument_noop() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        listener.onProcessingFailed(new DocumentProcessingFailedEvent(this, id, "boom"));

        verify(documentRepository, never()).save(any(Document.class));
    }
}
