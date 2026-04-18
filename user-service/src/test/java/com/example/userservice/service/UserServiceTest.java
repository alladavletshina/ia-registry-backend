package com.example.userservice.service;

import com.example.userservice.dto.request.RegisterRequestDto;
import com.example.userservice.dto.request.UserRequestDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.model.UserEntity;
import com.example.userservice.model.UserStatus;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminClient keycloakClient;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private UserService userService;

    private Jwt mockJwt;
    private UUID validUuid;

    @BeforeEach
    void setUp() {
        validUuid = UUID.randomUUID();
        mockJwt = mock(Jwt.class);
        // Стабы не делаем здесь – они будут в каждом тесте, где нужны
    }

    @Test
    void register_shouldCreateUserAndKeycloak() {
        // Даём реальный UUID от Keycloak
        String keycloakUuid = UUID.randomUUID().toString();
        when(keycloakClient.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUuid);

        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("123456789");
        request.setPosition("Developer");
        request.setDepartment("IT");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto result = userService.register(request, "127.0.0.1");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(keycloakClient).createRealmRole("user");
        verify(keycloakClient).assignRealmRole(keycloakUuid, "user");
        verify(auditEventPublisher).publishEvent(any());
    }

    @Test
    void getUserByKeycloakId_shouldReturnUser() {
        String keycloakId = UUID.randomUUID().toString();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setKeycloakId(keycloakId);
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));

        UserResponseDto result = userService.getUserByKeycloakId(keycloakId);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void updateUser_shouldModifyFields() {
        UUID userId = UUID.randomUUID();
        UserEntity existing = new UserEntity();
        existing.setId(userId);
        existing.setEmail("old@example.com");
        existing.setFirstName("Old");
        existing.setLastName("User");
        existing.setStatus(UserStatus.ACTIVE);

        UserRequestDto request = new UserRequestDto();
        request.setEmail("new@example.com");
        request.setFirstName("New");
        request.setLastName("Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(existing);

        // Настраиваем мок Jwt
        when(mockJwt.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(mockJwt.getClaim("preferred_username")).thenReturn("admin");

        UserResponseDto result = userService.updateUser(userId, request, "127.0.0.1", mockJwt);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        verify(auditEventPublisher).publishEvent(any());
    }
}