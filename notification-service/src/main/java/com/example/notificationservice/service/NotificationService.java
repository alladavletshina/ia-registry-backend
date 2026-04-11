package com.example.notificationservice.service;

import com.example.notificationservice.client.UserServiceClient;
import com.example.notificationservice.model.UserDto;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationCreateDto;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Jwt jwt, boolean unreadOnly, Pageable pageable) {
        UUID keycloakId = extractKeyclockId(jwt);
        log.info("Getting notifications for keycloakId: {}, isAdmin: {}", keycloakId, isAdmin(jwt));

        UUID internalUserId;
        if (isAdmin(jwt)) {
            internalUserId = keycloakId; // администратор использует свой Keycloak ID
            log.debug("Admin user, using keycloakId as internal ID: {}", internalUserId);
        } else {
            try {
                UserDto user = userServiceClient.getUserByKeycloakId(keycloakId.toString());
                if (user == null) {
                    log.warn("User not found for keycloakId: {}", keycloakId);
                    return Page.empty();
                }
                internalUserId = user.getId();
                log.debug("Regular user, internal ID: {}", internalUserId);
            } catch (Exception e) {
                log.error("Failed to fetch user from user-service: {}", e.getMessage(), e);
                return Page.empty();
            }
        }

        Page<Notification> page;
        if (unreadOnly) {
            page = notificationRepository.findByKeyclockIdAndReadFalse(internalUserId, pageable);
        } else {
            page = notificationRepository.findByKeyclockId(internalUserId, pageable);
        }
        return page.map(this::mapToDto);
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public NotificationDto createNotification(NotificationCreateDto dto) {
        Notification notification = Notification.builder()
                .keyclockId(dto.getKeyclockId())
                .type(dto.getType())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .actionUrl(dto.getActionUrl())
                .actionLabel(dto.getActionLabel())
                .read(false)
                .build();

        notification = notificationRepository.save(notification);
        return mapToDto(notification);
    }

    @Transactional(readOnly = true)
    public NotificationDto getNotificationById(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        return mapToDto(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Jwt jwt) {
        UUID keycloakId = extractKeyclockId(jwt);
        if (isAdmin(jwt)) {
            notificationRepository.markAllAsRead(keycloakId);
            log.info("Marked all notifications as read for admin: {}", keycloakId);
        } else {
            try {
                UserDto user = userServiceClient.getUserByKeycloakId(keycloakId.toString());
                if (user != null) {
                    notificationRepository.markAllAsRead(user.getId());
                    log.info("Marked all notifications as read for user: {}", keycloakId);
                } else {
                    log.warn("User not found for keycloakId: {}", keycloakId);
                }
            } catch (Exception e) {
                log.error("Failed to mark all as read for user {}: {}", keycloakId, e.getMessage(), e);
            }
        }
    }

    private NotificationDto mapToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setKeyclockId(notification.getKeyclockId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setActionUrl(notification.getActionUrl());
        dto.setActionLabel(notification.getActionLabel());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public long countUnread(Jwt jwt) {
        UUID keycloakId = extractKeyclockId(jwt);
        if (isAdmin(jwt)) {
            return notificationRepository.countByKeyclockIdAndReadFalse(keycloakId);
        } else {
            try {
                UserDto user = userServiceClient.getUserByKeycloakId(keycloakId.toString());
                if (user == null) return 0;
                return notificationRepository.countByKeyclockIdAndReadFalse(user.getId());
            } catch (Exception e) {
                log.error("Failed to count unread for user {}: {}", keycloakId, e.getMessage(), e);
                return 0;
            }
        }
    }

    public UUID extractKeyclockId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getAllNotifications(Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findAllByReadFalse(pageable);
        } else {
            page = notificationRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    private boolean isAdmin(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return false;
        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List<?>) {
            List<?> roles = (List<?>) rolesObj;
            return roles.contains("admin");
        }
        return false;
    }
}