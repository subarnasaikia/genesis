package com.genesis.importexport.event;

import com.genesis.importexport.format.Conll2012Parser.MentionSpan;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Published after a CoNLL-2012 file is parsed and its tokens/sentences
 * are persisted, so downstream modules (e.g. genesis-coref) can persist
 * the mention spans into their own tables.
 */
public class ConllImportedEvent extends ApplicationEvent {

    private final UUID documentId;
    private final UUID workspaceId;
    private final List<MentionSpan> mentionSpans;

    public ConllImportedEvent(Object source, UUID documentId, UUID workspaceId, List<MentionSpan> mentionSpans) {
        super(source);
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.mentionSpans = mentionSpans;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public List<MentionSpan> getMentionSpans() {
        return mentionSpans;
    }
}
