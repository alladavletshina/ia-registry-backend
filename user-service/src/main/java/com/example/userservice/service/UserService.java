package com.example.userservice.service;

import com.example.userservice.dto.request.RegisterRequestDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.model.UserEntity;
import com.example.userservice.model.UserStatus;
import com.example.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakClient;

    public UserResponseDto register(RegisterRequestDto request) {

        //Проверяем существует ли такой пользователь
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Пользователь с email " + request.getEmail() + " уже существует");
        }

        //Создаем временную запись в нашей БД (статус PENDING)
        UserEntity user = new UserEntity();

        user.setId(UUID.randomUUID());
        user.setEmail(request .getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setPosition(request.getPosition());
        user.setDepartment(request.getDepartment());
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);
        log.info("Создана временная запись пользователя: {}", savedUser.getId());

        try {
            //Создаем пользователя в Keycloak
            String keycloakUserId = keycloakClient.createUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );
            log.info("Пользователь создан в Keycloak: {}", keycloakUserId);

            //Обновляем запись в своей БД
            savedUser.setKeycloakId(keycloakUserId);
            savedUser.setStatus(UserStatus.ACTIVE);
            savedUser.setUpdatedAt(LocalDateTime.now());

            UserEntity finalUser = userRepository.save(savedUser);
            log.info("Запись пользователя активирована: {}", finalUser.getId());

            //Публикуем событие в RabbitMQ
            /*UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(finalUser.getId())
                    .keycloakId(keycloakUserId)
                    .email(finalUser.getEmail())
                    .firstName(finalUser.getFirstName())
                    .lastName(finalUser.getLastName())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishUserRegistered(event);
            log.info("Опубликовано событие user.registered для: {}", finalUser.getEmail());*/

            return mapToDto(finalUser);
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя: {}", e.getMessage(), e);

            // Откатываем запись в своей БД (помечаем как FAILED)
            savedUser.setStatus(UserStatus.FAILED);
            savedUser.setErrorMessage(e.getMessage());
            userRepository.save(savedUser);

            throw new RuntimeException("Не удалось зарегистрировать пользователя: " + e.getMessage(), e);
        }

    }

    private UserResponseDto mapToDto(UserEntity user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .position(user.getPosition())
                .department(user.getDepartment())
                .active(UserStatus.ACTIVE.equals(user.getStatus()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
