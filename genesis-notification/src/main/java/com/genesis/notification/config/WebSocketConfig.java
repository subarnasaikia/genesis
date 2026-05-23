package com.genesis.notification.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * Resolved at boot from the {@code cors.allowed-origins} property — the same
     * property the HTTP CORS filter ({@code SecurityConfig}) uses. Reusing it
     * guarantees the WebSocket handshake honours the same allow-list and closes
     * the CSWSH hole that {@code setAllowedOriginPatterns("*")} left wide open
     * (SECURITY_AUDIT HIGH-3 / ARCHITECTURE_AUDIT A-010 / SYSTEM_DESIGN_AUDIT P0#3).
     */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = parseOrigins(allowedOrigins);
        // SockJS fallback negotiates over HTTP and honours the exact-match allow-list.
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS();
        // Raw WebSocket endpoint — same allow-list, no wildcards.
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    private static String[] parseOrigins(String raw) {
        List<String> origins = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "cors.allowed-origins is empty or blank — set CORS_ALLOWED_ORIGINS to a comma-separated list of frontend origins");
        }
        return origins.toArray(new String[0]);
    }
}
