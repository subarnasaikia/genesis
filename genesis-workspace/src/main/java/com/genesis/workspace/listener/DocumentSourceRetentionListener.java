package com.genesis.workspace.listener;

import com.genesis.infra.storage.FileStorageService;
import com.genesis.infra.storage.StorageProperties;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.service.DocumentSourceReclaimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reclaims a document's raw source file once it has been tokenized, when the
 * deployment opts out of keeping sources ({@code genesis.storage.retain-source=false}).
 *
 * <p>
 * Safe because every read path after tokenization (editor, annotation, export)
 * runs off the database token rows — only re-tokenization needs the original. The
 * work is split across two committed transactions: {@link DocumentSourceReclaimer}
 * clears the FK first, then the physical file is deleted. Any failure here is
 * logged and swallowed so it never disrupts the tokenization-complete flow.
 */
@Component
public class DocumentSourceRetentionListener {

    private static final Logger logger =
            LoggerFactory.getLogger(DocumentSourceRetentionListener.class);

    private final DocumentSourceReclaimer reclaimer;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    public DocumentSourceRetentionListener(DocumentSourceReclaimer reclaimer,
            FileStorageService fileStorageService,
            StorageProperties storageProperties) {
        this.reclaimer = reclaimer;
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
    }

    @EventListener
    public void onTokenized(DocumentTokenizedEvent event) {
        if (storageProperties.isRetainSource()) {
            return;
        }
        try {
            reclaimer.detachSource(event.getDocumentId()).ifPresent(storedFileId -> {
                try {
                    fileStorageService.delete(storedFileId);
                    logger.info("Reclaimed source file {} after tokenizing document {} (retain-source=false)",
                            storedFileId, event.getDocumentId());
                } catch (RuntimeException ex) {
                    logger.warn("Failed to delete reclaimed source file {} for document {}: {}",
                            storedFileId, event.getDocumentId(), ex.getMessage());
                }
            });
        } catch (RuntimeException ex) {
            logger.warn("Source retention failed for document {}: {}",
                    event.getDocumentId(), ex.getMessage());
        }
    }
}
