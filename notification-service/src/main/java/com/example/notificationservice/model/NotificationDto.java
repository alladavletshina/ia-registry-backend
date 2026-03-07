package com.example.notificationservice.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationDto {
    private UUID id;
    private UUID keyclockId;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private String actionLabel;
    private boolean read;
    private LocalDateTime createdAt;
}
