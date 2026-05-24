package com.genesis.infra.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for JWT-based authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPoint authEntryPoint;

    /**
     * Resolved at boot from the {@code cors.allowed-origins} property. The
     * dev profile (application.properties) falls back to localhost; the prod
     * profile (application-prod.properties) has no fallback, so a missing
     * {@code CORS_ALLOWED_ORIGINS} env var fails boot with a clear
     * "Could not resolve placeholder" error instead of silently allowing
     * localhost in production.
     */
    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthEntryPoint authEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF (not needed for JWT)
                .csrf(AbstractHttpConfigurer::disable)
                // Browser security response headers
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(ref -> ref.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults()))
                // Stateless session (no cookies)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Exception handling
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPoint))
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints (signup, login, refresh, logout)
                        .requestMatchers("/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        // Health endpoints
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        // WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()
                        // Share-link CoNLL download — auth is via the share token in the query string
                        .requestMatchers("/api/public/export/**").permitAll()
                        // All other endpoints require authentication (including /api/auth/me)
                        .anyRequest().authenticated())
                // Add JWT filter
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Belt-and-suspenders: even with the placeholder resolved (e.g. to the
        // literal empty string), refuse to wire up an allow-list that would
        // accept zero or only-blank origins. Surfaces misconfiguration at boot.
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "cors.allowed-origins is empty or blank — set CORS_ALLOWED_ORIGINS to a comma-separated list of frontend origins");
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        // Expose Content-Disposition header for file downloads
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
