package com.genesis.common.port;

import java.util.UUID;

/**
 * Outbound port exposing the read-only token facts that annotation modules
 * (pos/ner/wsd) need without depending on {@code genesis-import-export}'s
 * repository or entity packages (ARCHITECTURE_AUDIT A-002/A-005).
 *
 * <p>Defined in the shared kernel because several feature modules need it; the
 * adapter is wired in {@code genesis-api} over {@code TokenRepository}.
 */
public interface TokenQueryPort {

    /**
     * Resolves the document a token belongs to.
     *
     * @param tokenId the token id
     * @return the owning document id
     * @throws com.genesis.common.exception.ResourceNotFoundException if no token
     *     with that id exists
     */
    UUID documentIdForToken(UUID tokenId);

    /**
     * Returns the surface form (word text) of a token.
     *
     * @param tokenId the token id
     * @return the token's form
     * @throws com.genesis.common.exception.ResourceNotFoundException if no token
     *     with that id exists
     */
    String formForToken(UUID tokenId);

    /**
     * @param documentId the document id
     * @return the number of tokens belonging to the document (0 if none)
     */
    long countTokensForDocument(UUID documentId);
}
