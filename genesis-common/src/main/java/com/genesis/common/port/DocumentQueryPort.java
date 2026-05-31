package com.genesis.common.port;

import java.util.UUID;

/**
 * Outbound port exposing the read-only document facts that annotation modules
 * (pos/ner/wsd) need without depending on {@code genesis-workspace}'s repository
 * or entity packages (ARCHITECTURE_AUDIT A-002/A-005).
 *
 * <p>Defined in the shared kernel because several feature modules need it; the
 * adapter is wired in {@code genesis-api} over {@code DocumentRepository}.
 */
public interface DocumentQueryPort {

    /**
     * Resolves the workspace that owns a document.
     *
     * @param documentId the document id
     * @return the owning workspace id, or {@code null} if the document exists but
     *     is not bound to a workspace
     * @throws com.genesis.common.exception.ResourceNotFoundException if no document
     *     with that id exists
     */
    UUID workspaceIdForDocument(UUID documentId);
}
