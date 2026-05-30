package com.genesis.importexport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.infra.storage.FileStorageService;
import com.genesis.workspace.event.DocumentProcessingFailedEvent;
import com.genesis.workspace.event.DocumentProcessingStartedEvent;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.event.DocumentUploadedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for {@link AsyncDocumentProcessor}. After A-006 the processor owns
 * no workspace data — it drives the document status lifecycle purely by
 * publishing events, which is what these tests assert.
 */
@ExtendWith(MockitoExtension.class)
class AsyncDocumentProcessorTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ImportService importService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AsyncDocumentProcessor processor;

    private DocumentUploadedEvent uploadEvent(String fileName) {
        return new DocumentUploadedEvent(
                this, UUID.randomUUID(), UUID.randomUUID(), "https://files/" + fileName, UUID.randomUUID(), fileName);
    }

    private List<ApplicationEvent> capturePublished() {
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("success - publishes Started then Tokenized (carrying the filename)")
    void success_publishesStartedThenTokenized() {
        DocumentUploadedEvent event = uploadEvent("notes.txt");
        when(fileStorageService.downloadAsString(event.getStoredFileUrl())).thenReturn("hello world");
        when(importService.importPlainText(event.getDocumentId(), "hello world"))
                .thenReturn(new ImportService.ImportResult(1, 2));

        processor.handleDocumentUploaded(event);

        List<ApplicationEvent> published = capturePublished();
        assertThat(published.get(0)).isInstanceOf(DocumentProcessingStartedEvent.class);
        assertThat(published.get(1)).isInstanceOf(DocumentTokenizedEvent.class);
        DocumentTokenizedEvent tokenized = (DocumentTokenizedEvent) published.get(1);
        assertThat(tokenized.getDocumentId()).isEqualTo(event.getDocumentId());
        assertThat(tokenized.getDocumentName()).isEqualTo("notes.txt");
    }

    @Test
    @DisplayName("failure - publishes Started then Failed (with the error)")
    void failure_publishesStartedThenFailed() {
        DocumentUploadedEvent event = uploadEvent("broken.txt");
        when(fileStorageService.downloadAsString(event.getStoredFileUrl()))
                .thenThrow(new RuntimeException("storage down"));

        processor.handleDocumentUploaded(event);

        List<ApplicationEvent> published = capturePublished();
        assertThat(published.get(0)).isInstanceOf(DocumentProcessingStartedEvent.class);
        assertThat(published.get(1)).isInstanceOf(DocumentProcessingFailedEvent.class);
        DocumentProcessingFailedEvent failed = (DocumentProcessingFailedEvent) published.get(1);
        assertThat(failed.getDocumentId()).isEqualTo(event.getDocumentId());
        assertThat(failed.getErrorMessage()).contains("storage down");
    }

    @Test
    @DisplayName("CoNLL file - routes through importConll2012 and publishes Tokenized")
    void conllFile_importedWithConllPath() throws Exception {
        DocumentUploadedEvent event = uploadEvent("doc.conll");
        when(fileStorageService.downloadAsString(event.getStoredFileUrl())).thenReturn("#begin document");
        when(importService.importConll2012(event.getDocumentId(), event.getWorkspaceId(), "#begin document"))
                .thenReturn(new ImportService.ImportResult(3, 9));

        processor.handleDocumentUploaded(event);

        List<ApplicationEvent> published = capturePublished();
        assertThat(published.get(0)).isInstanceOf(DocumentProcessingStartedEvent.class);
        assertThat(published.get(1)).isInstanceOf(DocumentTokenizedEvent.class);
    }

    @Test
    @DisplayName("CoNLL parse error - IOException is wrapped and surfaces as Failed")
    void conllParseError_publishesFailed() throws Exception {
        DocumentUploadedEvent event = uploadEvent("doc.conll");
        when(fileStorageService.downloadAsString(event.getStoredFileUrl())).thenReturn("#begin document");
        when(importService.importConll2012(event.getDocumentId(), event.getWorkspaceId(), "#begin document"))
                .thenThrow(new java.io.IOException("bad grid"));

        processor.handleDocumentUploaded(event);

        List<ApplicationEvent> published = capturePublished();
        assertThat(published.get(1)).isInstanceOf(DocumentProcessingFailedEvent.class);
        assertThat(((DocumentProcessingFailedEvent) published.get(1)).getErrorMessage())
                .contains("Failed to parse CoNLL file");
    }

    @Test
    @DisplayName("failure - over-long error is truncated; null message becomes 'Unknown error'")
    void failure_truncatesAndDefaultsErrorMessage() {
        // Over-long message → truncated to 900 chars + ellipsis.
        DocumentUploadedEvent longErr = uploadEvent("a.txt");
        when(fileStorageService.downloadAsString(longErr.getStoredFileUrl()))
                .thenThrow(new RuntimeException("x".repeat(1000)));
        processor.handleDocumentUploaded(longErr);
        DocumentProcessingFailedEvent failedLong = (DocumentProcessingFailedEvent) capturePublished().get(1);
        assertThat(failedLong.getErrorMessage()).hasSize(903).endsWith("...");

        // Null message → "Unknown error".
        org.mockito.Mockito.reset(eventPublisher);
        DocumentUploadedEvent nullErr = uploadEvent("b.txt");
        when(fileStorageService.downloadAsString(nullErr.getStoredFileUrl()))
                .thenThrow(new RuntimeException());
        processor.handleDocumentUploaded(nullErr);
        DocumentProcessingFailedEvent failedNull = (DocumentProcessingFailedEvent) capturePublished().get(1);
        assertThat(failedNull.getErrorMessage()).isEqualTo("Unknown error");
    }
}
