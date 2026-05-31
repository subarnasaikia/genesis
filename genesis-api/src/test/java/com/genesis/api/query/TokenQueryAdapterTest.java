package com.genesis.api.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.repository.TokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TokenQueryAdapter} — the composition-root adapter that
 * backs the annotation modules' {@link com.genesis.common.port.TokenQueryPort}
 * (A-002/A-005). Covers the shared {@code load()} not-found branch (used by both
 * documentIdForToken and formForToken) plus the count delegation.
 */
@ExtendWith(MockitoExtension.class)
class TokenQueryAdapterTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenQueryAdapter adapter;

    @Test
    @DisplayName("documentIdForToken - returns the token's document id")
    void documentIdForToken_resolves() {
        UUID tokenId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        TokenEntity token = mock(TokenEntity.class);
        when(token.getDocumentId()).thenReturn(documentId);
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertThat(adapter.documentIdForToken(tokenId)).isEqualTo(documentId);
    }

    @Test
    @DisplayName("documentIdForToken - missing token throws ResourceNotFoundException")
    void documentIdForToken_missing_throws() {
        UUID tokenId = UUID.randomUUID();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.documentIdForToken(tokenId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(tokenId.toString());
    }

    @Test
    @DisplayName("formForToken - returns the token's surface form")
    void formForToken_resolves() {
        UUID tokenId = UUID.randomUUID();
        TokenEntity token = mock(TokenEntity.class);
        when(token.getForm()).thenReturn("bank");
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertThat(adapter.formForToken(tokenId)).isEqualTo("bank");
    }

    @Test
    @DisplayName("formForToken - missing token throws ResourceNotFoundException")
    void formForToken_missing_throws() {
        UUID tokenId = UUID.randomUUID();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.formForToken(tokenId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("countTokensForDocument - delegates to the repository")
    void countTokensForDocument_delegates() {
        UUID documentId = UUID.randomUUID();
        when(tokenRepository.countByDocumentId(documentId)).thenReturn(42L);

        assertThat(adapter.countTokensForDocument(documentId)).isEqualTo(42L);
    }
}
