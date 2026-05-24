package com.genesis.notification.repository;

import com.genesis.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
    long countByRecipientIdAndReadFalse(UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);
}
