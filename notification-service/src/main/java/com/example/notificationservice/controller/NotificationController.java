package com.example.notificationservice.controller;

import com.example.notificationservice.model.NotificationCreateDto;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Управление уведомлениями пользователя")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Получить уведомления текущего пользователя (с пагинацией)")
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @RequestParam(required = false) boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID keyclockId = notificationService.extractKeyclockId(jwt);
        Page<NotificationDto> page = notificationService.getUserNotifications(keyclockId, unreadOnly, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить уведомление по ИД")
    public ResponseEntity<NotificationDto> getNotificationById(
            @RequestParam UUID notificationId
    ) {
        NotificationDto dto = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/read/{id}")
    @Operation(summary = "Отметить уведомление как прочитанное")
    public ResponseEntity<Void> markAsRead(
            @RequestParam UUID notificationId
    ) {
        notificationService.markAsRead(notificationId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Отметить все уведомления как прочитанные")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt
    ){

        UUID keyclock = notificationService.extractKeyclockId(jwt);
        notificationService.markAllAsRead(keyclock);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить уведомление")
    public ResponseEntity<Void> deleteNotification(
            @RequestParam UUID notificationId
    ) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Тестовый эндпоинт для создания уведомления (без RabbitMQ)")
    public ResponseEntity<NotificationDto> createNotification(
            @RequestParam NotificationType type,
            @RequestParam String title,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String actionUrl,
            @RequestParam(required = false) String actionLabel,
            @AuthenticationPrincipal Jwt jwt
            ) {

        UUID keyclockId = notificationService.extractKeyclockId(jwt);

        NotificationCreateDto dto = new NotificationCreateDto();
        dto.setKeyclockId(keyclockId);
        dto.setType(type);
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setActionUrl(actionUrl);
        dto.setActionLabel(actionLabel);
        dto.setRead(false);

        NotificationDto notification = notificationService.createNotification(dto);
        return ResponseEntity.ok(notification);
    }
}
