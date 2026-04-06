package com.example.userservice.controller;

import com.example.userservice.dto.request.ChangePasswordRequest;
import com.example.userservice.dto.request.UserRequestDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.service.UserService;
import com.example.userservice.util.IpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Управление пользователями")
public class UserController {

    private final UserService userService;

    @GetMapping("/report/csv")
    public void exportUsersToCsv(HttpServletResponse response) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users_report.csv");

        try (OutputStream os = response.getOutputStream()) {
            userService.exportUsersToCsv(os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить список всех пользователей", description = "Только для администраторов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей получен",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {

        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить данные по пользователю по id", description = "Только для администраторов")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    @Operation(summary = "Получить информацию о текущем пользователе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован (отсутствует или недействителен токен)"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден в базе данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        log.info("Запрос текущего пользователя по keycloakId: {}", keycloakId);
        UserResponseDto user = userService.getUserByKeycloakId(keycloakId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    @Operation(summary = "Обновить данные пользователя (админ)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь обновлён",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные (например, email уже занят)"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка")
    })
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID id,
                                                      @Valid @RequestBody UserRequestDto request,
                                                      HttpServletRequest httpRequest,
                                                      @AuthenticationPrincipal Jwt jwt
                                                      ) {
        String clientIp = IpUtils.getClientIp(httpRequest);
        UserResponseDto updated = userService.updateUser(id, request,clientIp, jwt);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('user')")
    @Operation(summary = "Обновить данные пользователя (пользователь)")
    public ResponseEntity<UserResponseDto> updateCurrentUser(@RequestBody UserRequestDto request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        UserResponseDto updated = userService.updateMe(request, jwt);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    @Operation(summary = "Смена пароля текущего пользователя")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        userService.changePassword(request.getOldPassword(), request.getNewPassword(), jwt);
        return ResponseEntity.ok().build();
    }

}
