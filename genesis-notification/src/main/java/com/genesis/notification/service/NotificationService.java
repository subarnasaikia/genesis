package com.genesis.notification.service;

import com.genesis.notification.dto.NotificationDTO;
import com.genesis.notification.entity.Notification;
import com.genesis.notification.entity.NotificationType;
import com.genesis.notification.repository.NotificationRepository;
import com.genesis.user.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, 
                               SimpMessagingTemplate messagingTemplate,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(UUID recipientId, String title, String message, NotificationType type, UUID workspaceId, UUID actorId, String link) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setWorkspaceId(workspaceId);
        notification.setActorId(actorId);
        notification.setLink(link);
        
        notification = notificationRepository.save(notification);

        NotificationDTO dto = mapToDTO(notification);
        
        // Look up username for WebSocket routing (convertAndSendToUser uses username, not UUID)
        userRepository.findById(recipientId).ifPresent(user -> {
            messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                dto
            );
        });
    }

    public List<NotificationDTO> getUserNotifications(UUID userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void delete(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setLink(notification.getLink());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setWorkspaceId(notification.getWorkspaceId());
        dto.setActorId(notification.getActorId());
        return dto;
    }
}
