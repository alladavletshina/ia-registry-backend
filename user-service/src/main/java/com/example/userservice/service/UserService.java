package com.example.userservice.service;

import com.example.userservice.dto.request.AuditEventDto;
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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
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
    private final AuditEventPublisher auditEventPublisher;

    public UserResponseDto register(RegisterRequestDto request, String clientIp) {

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
        user.setRole("USER");
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

            // Создаём роль "user", если её нет
            keycloakClient.createRealmRole("user");
            // Назначаем роль пользователю
            keycloakClient.assignRealmRole(keycloakUserId, "user");

            //Обновляем запись в своей БД
            savedUser.setKeycloakId(keycloakUserId);
            savedUser.setStatus(UserStatus.ACTIVE);
            savedUser.setUpdatedAt(LocalDateTime.now());

            UserEntity finalUser = userRepository.save(savedUser);
            log.info("Запись пользователя активирована: {}", finalUser.getId());

            AuditEventDto event = new AuditEventDto();
            event.setUserId(UUID.fromString(keycloakUserId));
            event.setUsername(finalUser.getEmail());
            event.setAction("USER_REGISTER");
            event.setDetails(String.format("Зарегистрирован пользователь: %s %s (%s)", finalUser.getFirstName(), finalUser.getLastName(), finalUser.getEmail()));
            event.setSeverity("INFO");
            event.setServiceName("user-service");
            event.setObjectId(finalUser.getId().toString());
            event.setObjectType("User");
            event.setIp(clientIp);
            auditEventPublisher.publishEvent(event);

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
                .role(user.getRole())
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

    public UserResponseDto updateUser(UUID id, @Valid UserRequestDto request, String clientIp, Jwt jwt) {

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
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            user.setStatus(request.getActive() ? UserStatus.ACTIVE : UserStatus.BLOCKED);
        }

        user.setUpdatedAt(LocalDateTime.now());
        UserEntity updatedUser = userRepository.save(user);

        AuditEventDto event = new AuditEventDto();
        event.setUserId(UUID.fromString(jwt.getSubject()));
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("USER_UPDATE");
        event.setDetails(String.format("Обновлен пользователь id=%s: %s %s (%s)", id, user.getFirstName(), user.getLastName(), user.getEmail()));
        event.setSeverity("INFO");
        event.setServiceName("user-service");
        event.setObjectId(id.toString());
        event.setObjectType("User");
        event.setIp(clientIp);
        auditEventPublisher.publishEvent(event);
        return mapToDto(updatedUser);

    }

    public UserResponseDto getUserById(UUID id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с id: " + id));

        return mapToDto(user);
    }

    public void exportUsersToCsv(OutputStream os) {

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            writer.print('\uFEFF');

            writer.println("ID,Email,Имя,Фамилия,Телефон,Должность,Отдел,Роль,Статус,Дата создания,Дата обновления");

            List<UserEntity> users = userRepository.findAll();

            for (UserEntity user : users) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        user.getId(),
                        escapeCsv(user.getEmail()),
                        escapeCsv(user.getFirstName()),
                        escapeCsv(user.getLastName()),
                        escapeCsv(user.getPhone()),
                        escapeCsv(user.getPosition()),
                        escapeCsv(user.getDepartment()),
                        user.getRole(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ));
            }
            writer.flush();
        }
    }

    /**
     * Экранирование значений для корректного CSV.
     * Оборачивает в кавычки, если значение содержит запятые, кавычки или переводы строк.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public UserResponseDto updateMe(UserRequestDto request, Jwt jwt) {

        String keycloakId = jwt.getSubject();

        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setPosition(request.getPosition());
        user.setDepartment(request.getDepartment());

        userRepository.save(user);
        return mapToDto(user);
    }

    /**
     * Валидация сложности пароля (дублирует аннотации из DTO для надёжности)
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен содержать минимум 8 символов");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен содержать хотя бы одну цифру");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен содержать хотя бы одну строчную букву");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен содержать хотя бы одну заглавную букву");
        }
        if (!password.matches(".*[@#$%^&+=!].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен содержать хотя бы один спецсимвол (@ # $ % ^ & + = !)");
        }
    }

    /**
     * Смена пароля текущим пользователем (требуется старый пароль)
     */
    public void changePassword(String oldPassword, String newPassword, Jwt jwt, String clientIp) {
        String keycloakId = jwt.getSubject();
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем старый пароль через Keycloak
        boolean valid = keycloakClient.verifyPassword(user.getEmail(), oldPassword);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный текущий пароль");
        }

        if (oldPassword.equals(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Новый пароль не должен совпадать со старым");
        }

        // Валидация сложности нового пароля
        validatePasswordStrength(newPassword);

        // Устанавливаем новый пароль
        keycloakClient.setUserPassword(keycloakId, newPassword);

        // Отправляем событие аудита (опционально)
        AuditEventDto event = new AuditEventDto();
        event.setUserId(UUID.fromString(keycloakId));
        event.setUsername(user.getEmail());
        event.setAction("PASSWORD_CHANGE");
        event.setDetails("Пользователь сменил пароль");
        event.setSeverity("WARNING");
        event.setServiceName("user-service");
        event.setObjectId(user.getId().toString());
        event.setIp(clientIp);
        event.setObjectType("User");
        auditEventPublisher.publishEvent(event);
    }
}
