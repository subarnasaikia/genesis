package com.genesis.common.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Published by {@code genesis-coref} when a document's mentions change (create,
 * assign, unassign). Carries the document id and the current total mention-token
 * count for that document so {@code genesis-workspace} can recompute the
 * document's annotation progress/status from its own entity — without coref
 * reaching into the workspace {@code DocumentService} (ARCHITECTURE_AUDIT A-001).
 *
 * <p>Lives in {@code genesis-common} (the shared kernel) because the publisher
 * (coref) and the listener (workspace) are sibling modules that do not depend on
 * each other; this mirrors {@code WorkspaceActivityEvent}/{@code AnnotationLogEvent}.
 */
public class MentionAnnotatedEvent extends ApplicationEvent {

    private final UUID documentId;
    private final long mentionTokenCount;

    public MentionAnnotatedEvent(Object source, UUID documentId, long mentionTokenCount) {
        super(source);
        this.documentId = documentId;
        this.mentionTokenCount = mentionTokenCount;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    /** Total number of tokens covered by mentions on this document. */
    public long getMentionTokenCount() {
        return mentionTokenCount;
    }
}
