package com.genesis.notification.dto;

import com.genesis.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDTO {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
    private UUID workspaceId;
    private UUID actorId;
}
