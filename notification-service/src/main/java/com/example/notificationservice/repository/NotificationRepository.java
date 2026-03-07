package com.example.notificationservice.repository;

import com.example.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByKeyclockId(UUID keyclockId, Pageable pageable);

    Page<Notification> findByKeyclockIdAndReadFalse(UUID keyclockId, Pageable pageable);

}
