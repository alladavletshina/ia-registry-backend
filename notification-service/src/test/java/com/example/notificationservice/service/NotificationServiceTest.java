package com.example.notificationservice.service;

import com.example.notificationservice.client.UserServiceClient;
import com.example.notificationservice.model.*;
import com.example.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private NotificationService notificationService;

    private Jwt mockJwt(UUID userId, boolean isAdmin) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        if (isAdmin) {
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("admin")));
        } else {
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("user")));
        }
        return jwt;
    }

    @Test
    void createNotification_shouldSaveAndReturnDto() {
        UUID userId = UUID.randomUUID();
        NotificationCreateDto createDto = new NotificationCreateDto();
        createDto.setKeyclockId(userId);
        createDto.setType(NotificationType.INFO);
        createDto.setTitle("Test Title");
        createDto.setMessage("Test Message");

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .keyclockId(userId)
                .type(NotificationType.INFO)
                .title("Test Title")
                .message("Test Message")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.save(any(Notification.class))).thenReturn(saved);

        NotificationDto result = notificationService.createNotification(createDto);
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.isRead()).isFalse();
        verify(repository).save(any(Notification.class));
    }

    @Test
    void getUserNotifications_asRegularUser_shouldMapInternalUserId() {
        UUID keycloakId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        Jwt jwt = mockJwt(keycloakId, false);
        UserDto userDto = new UserDto();
        userDto.setId(internalId);

        when(userServiceClient.getUserByKeycloakId(keycloakId.toString())).thenReturn(userDto);
        when(repository.findByKeyclockId(eq(internalId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<NotificationDto> page = notificationService.getUserNotifications(jwt, false, PageRequest.of(0, 10));
        assertThat(page).isNotNull();
        verify(repository).findByKeyclockId(internalId, PageRequest.of(0, 10));
    }

    @Test
    void markAsRead_shouldSetReadFlag() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder().id(notificationId).read(false).build();
        when(repository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId);

        assertThat(notification.isRead()).isTrue();
        verify(repository).save(notification);
    }

    @Test
    void countUnread_forRegularUser_shouldReturnFromRepository() {
        UUID keycloakId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        Jwt jwt = mockJwt(keycloakId, false);
        UserDto userDto = new UserDto();
        userDto.setId(internalId);

        when(userServiceClient.getUserByKeycloakId(keycloakId.toString())).thenReturn(userDto);
        when(repository.countByKeyclockIdAndReadFalse(internalId)).thenReturn(5L);

        long count = notificationService.countUnread(jwt);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void deleteNotification_shouldCallRepositoryDelete() {
        UUID id = UUID.randomUUID();
        notificationService.deleteNotification(id);
        verify(repository).deleteById(id);
    }
}