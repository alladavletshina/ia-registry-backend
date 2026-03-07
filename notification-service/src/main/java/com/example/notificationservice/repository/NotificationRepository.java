package com.example.notificationservice.repository;

import com.example.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByKeyclockId(UUID keyclockId, Pageable pageable);

    Page<Notification> findByKeyclockIdAndReadFalse(UUID keyclockId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.keyclockId = :userId AND n.read = false")
    void markAllAsRead(@Param("userId") UUID userId);

    long countByKeyclockIdAndReadFalse(UUID keyclockId);
}
