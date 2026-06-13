package com.genesis.workspace.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.infra.storage.FileStorageService;
import com.genesis.infra.storage.StorageProperties;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.service.DocumentSourceReclaimer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSourceRetentionListener Tests")
class DocumentSourceRetentionListenerTest {

    @Mock
    private DocumentSourceReclaimer reclaimer;

    @Mock
    private FileStorageService fileStorageService;

    private StorageProperties storageProperties;
    private DocumentSourceRetentionListener listener;

    private final UUID documentId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        listener = new DocumentSourceRetentionListener(reclaimer, fileStorageService, storageProperties);
    }

    private DocumentTokenizedEvent event() {
        return new DocumentTokenizedEvent(this, documentId, workspaceId, "sample.txt");
    }

    @Test
    @DisplayName("retain-source=true: does nothing")
    void retainKeepsSource() {
        storageProperties.setRetainSource(true);

        listener.onTokenized(event());

        verifyNoInteractions(reclaimer, fileStorageService);
    }

    @Test
    @DisplayName("retain-source=false: deletes the reclaimed source file")
    void deletesReclaimedSource() {
        storageProperties.setRetainSource(false);
        UUID storedFileId = UUID.randomUUID();
        when(reclaimer.detachSource(documentId)).thenReturn(Optional.of(storedFileId));

        listener.onTokenized(event());

        verify(fileStorageService).delete(storedFileId);
    }

    @Test
    @DisplayName("retain-source=false with no source: does not call delete")
    void noSourceNoDelete() {
        storageProperties.setRetainSource(false);
        when(reclaimer.detachSource(documentId)).thenReturn(Optional.empty());

        listener.onTokenized(event());

        verify(fileStorageService, never()).delete(any(UUID.class));
    }

    @Test
    @DisplayName("a delete failure is swallowed, not propagated")
    void deleteFailureSwallowed() {
        storageProperties.setRetainSource(false);
        UUID storedFileId = UUID.randomUUID();
        when(reclaimer.detachSource(documentId)).thenReturn(Optional.of(storedFileId));
        doThrow(new RuntimeException("backend down")).when(fileStorageService).delete(storedFileId);

        // Must not throw — retention is best-effort cleanup.
        listener.onTokenized(event());

        verify(fileStorageService).delete(storedFileId);
    }

    @Test
    @DisplayName("a reclaimer failure is swallowed, not propagated")
    void reclaimFailureSwallowed() {
        storageProperties.setRetainSource(false);
        when(reclaimer.detachSource(documentId)).thenThrow(new RuntimeException("db down"));

        listener.onTokenized(event());

        verifyNoInteractions(fileStorageService);
    }
}
