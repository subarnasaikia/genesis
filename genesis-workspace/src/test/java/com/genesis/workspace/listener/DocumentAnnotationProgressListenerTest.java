package com.genesis.workspace.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.common.event.MentionAnnotatedEvent;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.service.DocumentService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DocumentAnnotationProgressListener} — the workspace-side
 * owner of document status/progress that replaced coref's direct DocumentService
 * calls (A-001).
 */
@ExtendWith(MockitoExtension.class)
class DocumentAnnotationProgressListenerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentAnnotationProgressListener listener;

    private DocumentResponse doc(DocumentStatus status, int start, int end) {
        DocumentResponse d = new DocumentResponse();
        d.setStatus(status);
        d.setTokenStartIndex(start);
        d.setTokenEndIndex(end);
        return d;
    }

    @Test
    @DisplayName("first annotation on an UPLOADED doc → ANNOTATING + computed progress")
    void uploaded_transitionsAndComputesProgress() {
        UUID id = UUID.randomUUID();
        when(documentService.getByIdInternal(id)).thenReturn(doc(DocumentStatus.UPLOADED, 0, 9)); // 10 tokens

        listener.onMentionAnnotated(new MentionAnnotatedEvent(this, id, 3)); // 3/10

        verify(documentService).updateStatusInternal(id, DocumentStatus.ANNOTATING);
        verify(documentService).updateProgress(id, 0.3);
    }

    @Test
    @DisplayName("already ANNOTATING → no status change, progress still updated")
    void annotating_onlyUpdatesProgress() {
        UUID id = UUID.randomUUID();
        when(documentService.getByIdInternal(id)).thenReturn(doc(DocumentStatus.ANNOTATING, 0, 4)); // 5 tokens

        listener.onMentionAnnotated(new MentionAnnotatedEvent(this, id, 1)); // 1/5

        verify(documentService, never()).updateStatusInternal(eq(id), org.mockito.ArgumentMatchers.any());
        verify(documentService).updateProgress(id, 0.2);
    }

    @Test
    @DisplayName("more mention tokens than document tokens → progress capped at 1.0")
    void progressCappedAtOne() {
        UUID id = UUID.randomUUID();
        when(documentService.getByIdInternal(id)).thenReturn(doc(DocumentStatus.ANNOTATING, 0, 9)); // 10 tokens

        listener.onMentionAnnotated(new MentionAnnotatedEvent(this, id, 25)); // 25/10 → cap

        verify(documentService).updateProgress(id, 1.0);
    }

    @Test
    @DisplayName("zero-token document → progress stays 0.0 (no divide-by-zero)")
    void zeroTokenDocument_progressZero() {
        UUID id = UUID.randomUUID();
        // start==end+1 would be 0 tokens; use start=0,end=-1 → totalTokens = 0
        when(documentService.getByIdInternal(id)).thenReturn(doc(DocumentStatus.ANNOTATING, 0, -1));

        listener.onMentionAnnotated(new MentionAnnotatedEvent(this, id, 5));

        verify(documentService).updateProgress(id, 0.0);
    }

    @Test
    @DisplayName("null token indices (not yet tokenized) → progress 0.0, no NPE")
    void nullTokenIndices_progressZero() {
        UUID id = UUID.randomUUID();
        // A mention created before tokenization: token range is null. The old code
        // unboxed null Integer → NPE at the totalTokens arithmetic.
        DocumentResponse notTokenized = new DocumentResponse();
        notTokenized.setStatus(DocumentStatus.ANNOTATING);
        notTokenized.setTokenStartIndex(null);
        notTokenized.setTokenEndIndex(null);
        when(documentService.getByIdInternal(id)).thenReturn(notTokenized);

        listener.onMentionAnnotated(new MentionAnnotatedEvent(this, id, 5)); // must not throw

        verify(documentService).updateProgress(id, 0.0);
    }

    @Test
    @DisplayName("lookup failure is swallowed (log, don't fail) — no progress write, no throw")
    void lookupFailure_swallowed() {
        UUID id = UUID.randomUUID();
        when(documentService.getByIdInternal(id)).thenThrow(new RuntimeException("doc gone"));

        listener.onMentionAnnotated(new MentionAnnotatedEvent(this, id, 1)); // must not throw

        verify(documentService, never()).updateProgress(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble());
    }
}
