package com.genesis.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.user.entity.RefreshToken;
import com.genesis.user.entity.User;
import com.genesis.user.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RefreshTokenService}, focused on the refresh-token TTL
 * that is now injected via {@code @Value} rather than read from infra's
 * {@code SecurityProperties} (ARCHITECTURE_AUDIT A-007). The service is
 * constructed with a plain {@link Duration}, so the expiry behaviour is
 * verifiable without a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    /** save() echoes its argument back so callers can read the persisted token. */
    private void stubSaveEchoesArgument() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("createRefreshToken - applies the injected TTL to the expiry date")
    void createRefreshToken_appliesInjectedTtl() {
        stubSaveEchoesArgument();
        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository, Duration.ofDays(7));
        Instant before = Instant.now();

        RefreshToken token = service.createRefreshToken(mock(User.class));

        Instant after = Instant.now();
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.isRevoked()).isFalse();
        // expiry must fall within [before+7d, after+7d]
        assertThat(token.getExpiryDate())
                .isAfterOrEqualTo(before.plus(Duration.ofDays(7)))
                .isBeforeOrEqualTo(after.plus(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("createRefreshToken - honours a non-default TTL (proves the injected value is used)")
    void createRefreshToken_honoursCustomTtl() {
        stubSaveEchoesArgument();
        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository, Duration.ofMinutes(30));
        Instant before = Instant.now();

        RefreshToken token = service.createRefreshToken(mock(User.class));

        assertThat(token.getExpiryDate())
                .isAfterOrEqualTo(before.plus(Duration.ofMinutes(30)))
                .isBeforeOrEqualTo(Instant.now().plus(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("verifyExpiration - expired token is deleted and rejected")
    void verifyExpiration_expiredToken_throws() {
        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository, Duration.ofDays(7));
        RefreshToken expired = new RefreshToken();
        expired.setExpiryDate(Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> service.verifyExpiration(expired))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    @DisplayName("verifyExpiration - valid token is returned and not deleted")
    void verifyExpiration_validToken_returns() {
        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository, Duration.ofDays(7));
        RefreshToken valid = new RefreshToken();
        valid.setExpiryDate(Instant.now().plusSeconds(3600));

        assertThat(service.verifyExpiration(valid)).isSameAs(valid);
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }
}
