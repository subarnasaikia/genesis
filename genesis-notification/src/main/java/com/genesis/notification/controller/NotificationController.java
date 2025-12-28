package com.genesis.notification.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.notification.dto.NotificationDTO;
import com.genesis.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final com.genesis.user.repository.UserRepository userRepository;

    public NotificationController(NotificationService notificationService, com.genesis.user.repository.UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotifications(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        UUID userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotifications(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        UUID userId = getUserId(userDetails);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification deleted"));
    }

    private UUID getUserId(org.springframework.security.core.userdetails.UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
