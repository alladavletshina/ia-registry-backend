package com.example.notificationservice.controller;

import com.example.notificationservice.model.NotificationCreateDto;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.model.UnreadCountDto;
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
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @RequestParam(required = false) boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Page<NotificationDto> page = notificationService.getUserNotifications(jwt, unreadOnly, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить уведомление по ИД")
    public ResponseEntity<NotificationDto> getNotificationById(
            @PathVariable("id") UUID notificationId
    ) {
        NotificationDto dto = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/read/{id}")
    @Operation(summary = "Отметить уведомление как прочитанное")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") UUID notificationId
    ) {
        notificationService.markAsRead(notificationId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Отметить все уведомления как прочитанные")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Количество непрочитанных уведомлений")
    public ResponseEntity<UnreadCountDto> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        long count = notificationService.countUnread(jwt);
        return ResponseEntity.ok(new UnreadCountDto(count));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить уведомление")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID notificationId
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

    @GetMapping("/admin/all")
    @Operation(summary = "Получить все непрочитанные уведомления")
    public ResponseEntity<Page<NotificationDto>> getAllNotifications(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationDto> page = notificationService.getAllNotifications(true, pageable);
        return ResponseEntity.ok(page);
    }
}
