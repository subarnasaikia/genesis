package com.genesis.common.audit;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AuditorAware} for JPA auditing.
 *
 * <p>
 * Returns the current user for audit fields (createdBy, updatedBy).
 * Currently returns "system" as a placeholder until authentication is
 * integrated.
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM_USER = "system";

    @Override
    @NonNull
    @SuppressWarnings("null")
    public Optional<String> getCurrentAuditor() {
        // TODO: Integrate with Spring Security to get actual authenticated user
        // SecurityContextHolder.getContext().getAuthentication().getName()
        return Optional.of(SYSTEM_USER);
    }
}
