package com.genesis.api.query;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.port.TokenQueryPort;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.repository.TokenRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composition-root adapter implementing {@link TokenQueryPort} over
 * {@code genesis-import-export}'s {@code TokenRepository}. Keeps the cross-module
 * data-access reach inside {@code genesis-api} so annotation modules depend only
 * on the port (ARCHITECTURE_AUDIT A-002/A-005), mirroring {@code RecipientDirectoryAdapter}.
 */
@Component
public class TokenQueryAdapter implements TokenQueryPort {

    private final TokenRepository tokenRepository;

    public TokenQueryAdapter(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID documentIdForToken(UUID tokenId) {
        return load(tokenId).getDocumentId();
    }

    @Override
    @Transactional(readOnly = true)
    public String formForToken(UUID tokenId) {
        return load(tokenId).getForm();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTokensForDocument(UUID documentId) {
        return tokenRepository.countByDocumentId(documentId);
    }

    private TokenEntity load(UUID tokenId) {
        return tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found: " + tokenId));
    }
}
