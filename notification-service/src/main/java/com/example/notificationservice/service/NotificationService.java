package com.example.notificationservice.service;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationCreateDto;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<NotificationDto> getUserNotifications(UUID keyclockId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if(Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByKeyclockIdAndReadFalse(keyclockId, pageable);
        } else {
            page = notificationRepository.findByKeyclockId(keyclockId, pageable);
        }
        return page.map(this::mapToDo);
    }



    private NotificationDto mapToDo(Notification notification) {
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

    public UUID extractKeyclockId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }


    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

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

        NotificationDto notificationDto = mapToDo(notification);
        return notificationDto;
    }
}
