package com.genesis.api.security;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.user.service.UserService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Centralizes resolution of the authenticated caller from the Spring Security
 * context. This is the single place in {@code genesis-api} that touches
 * {@link SecurityContextHolder}; controllers depend on this component instead of
 * each maintaining their own duplicated principal-resolution helper.
 *
 * <p>Id resolution is delegated to {@link UserService} so that no JPA or
 * repository access leaks into the API layer.
 */
@Component
public class AuthenticatedUserResolver {

    private final UserService userService;

    public AuthenticatedUserResolver(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the authenticated caller's principal name (username).
     *
     * @return the authenticated username
     * @throws UnauthorizedException if there is no authenticated, non-anonymous
     *     principal
     */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("Authentication required");
        }
        return auth.getName();
    }

    /**
     * Returns the authenticated caller's user id.
     *
     * @return the authenticated user's id
     * @throws UnauthorizedException if there is no authenticated principal or no
     *     matching user
     */
    public UUID currentUserId() {
        return userService.getUserIdByUsername(currentUsername());
    }
}
