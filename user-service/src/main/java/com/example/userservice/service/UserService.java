package com.example.userservice.service;

import com.example.userservice.dto.request.RegisterRequestDto;
import com.example.userservice.dto.request.UserRequestDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.model.UserEntity;
import com.example.userservice.model.UserStatus;
import com.example.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<UserResponseDto> getAllUsers() {
        List<UserEntity> users =  userRepository.findAll();
        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
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
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserResponseDto getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с email: " + email));
        return mapToDto(user);
    }

    public UserResponseDto getUserByKeycloakId(String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с keycloakId: " + keycloakId));
        return mapToDto(user);
    }

    public UserResponseDto updateUser(UUID id, @Valid UserRequestDto request) {

        // 1. Найти пользователя
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с id: " + id));

        // 2. Проверить уникальность email, если он меняется
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email " + request.getEmail() + " уже используется");
            }
            user.setEmail(request.getEmail());
        }

        // 3. Обновить остальные поля (если они переданы)
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
//        if (request.getRole() != null) {
//            user.setRole(request.getRole());
//            // Если роль хранится в Keycloak, обновить её там
//            // keycloakClient.updateUserRole(user.getKeycloakId(), request.getRole());
//        }
        if (request.getActive() != null) {
            user.setStatus(request.getActive() ? UserStatus.ACTIVE : UserStatus.BLOCKED);
            // Если нужно, можно также деактивировать в Keycloak (например, отключить пользователя)
            // keycloakClient.enableUser(user.getKeycloakId(), request.getIsActive());
        }

        user.setUpdatedAt(LocalDateTime.now());
        UserEntity updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);

    }
}
