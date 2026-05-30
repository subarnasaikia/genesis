package com.genesis.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.notification.entity.Notification;
import com.genesis.notification.entity.NotificationType;
import com.genesis.notification.port.RecipientDirectory;
import com.genesis.notification.repository.NotificationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Unit tests for {@link NotificationService}'s WebSocket-routing path, which now
 * resolves the recipient username through {@link RecipientDirectory} instead of
 * a direct {@code UserRepository} (A-004).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RecipientDirectory recipientDirectory;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("createNotification - routes over WebSocket to the resolved username")
    void createNotification_routesToResolvedUsername() {
        UUID recipient = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(recipientDirectory.username(recipient)).thenReturn(Optional.of("ada"));

        notificationService.createNotification(
                recipient, "Title", "Body", NotificationType.INFO, null, null, "/home");

        verify(messagingTemplate).convertAndSendToUser(eq("ada"), eq("/queue/notifications"), any());
    }

    @Test
    @DisplayName("createNotification - missing username skips WebSocket routing, no throw")
    void createNotification_missingUsernameSkipsRouting() {
        UUID recipient = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(recipientDirectory.username(recipient)).thenReturn(Optional.empty());

        notificationService.createNotification(
                recipient, "Title", "Body", NotificationType.INFO, null, null, "/home");

        verify(messagingTemplate, never()).convertAndSendToUser(
                any(String.class), any(String.class), any());
    }
}
