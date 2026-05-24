package com.genesis.notification.dto;

import com.genesis.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
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
    private Instant createdAt;
    private UUID workspaceId;
    private UUID actorId;

    public NotificationDTO() {}

    public NotificationDTO(UUID id, NotificationType type, String title, String message, String link, boolean read, Instant createdAt, UUID workspaceId, UUID actorId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.read = read;
        this.createdAt = createdAt;
        this.workspaceId = workspaceId;
        this.actorId = actorId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
}
