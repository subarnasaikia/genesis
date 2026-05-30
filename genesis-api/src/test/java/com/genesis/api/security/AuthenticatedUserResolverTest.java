package com.genesis.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * Unit tests for {@link AuthenticatedUserResolver}. Drives the static
 * {@link SecurityContextHolder} directly and mocks {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticatedUserResolver resolver;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("currentUsername - no authentication - throws UnauthorizedException")
    void currentUsername_noAuth_throws() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> resolver.currentUsername())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("currentUsername - anonymous principal - throws UnauthorizedException")
    void currentUsername_anonymous_throws() {
        AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(anon);
        SecurityContextHolder.setContext(ctx);

        assertThatThrownBy(() -> resolver.currentUsername())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("currentUserId - unauthenticated token - throws UnauthorizedException")
    void currentUserId_unauthenticatedToken_throws() {
        // A token constructed without authorities is not authenticated.
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("alice", "creds");
        token.setAuthenticated(false);
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(token);
        SecurityContextHolder.setContext(ctx);

        assertThatThrownBy(() -> resolver.currentUserId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("currentUserId - authenticated user - resolves id via UserService")
    void currentUserId_authenticated_resolvesId() {
        UUID expected = UUID.randomUUID();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        "alice",
                        "creds",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(token);
        SecurityContextHolder.setContext(ctx);

        when(userService.getUserIdByUsername("alice")).thenReturn(expected);

        assertThat(resolver.currentUsername()).isEqualTo("alice");
        assertThat(resolver.currentUserId()).isEqualTo(expected);
    }

    @Test
    @DisplayName("currentUserId - authenticated but user missing - propagates UnauthorizedException")
    void currentUserId_userMissing_propagates() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        "ghost",
                        "creds",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(token);
        SecurityContextHolder.setContext(ctx);

        when(userService.getUserIdByUsername("ghost"))
                .thenThrow(new UnauthorizedException("User not found"));

        assertThatThrownBy(() -> resolver.currentUserId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found");
    }
}
