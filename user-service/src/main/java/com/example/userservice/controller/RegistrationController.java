package com.example.userservice.controller;

import com.example.userservice.dto.request.RegisterRequestDto;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Регистрация", description = "Управление регистрацией пользователей")
public class RegistrationController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация нового пользователя",
            description = "Создаёт нового пользователя в системе и в Keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует",
                    content = @Content)
    })
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest
    ) {
        log.info("Регистрация нового пользователя: {}", request.getEmail());

        String clientIp = IpUtils.getClientIp(httpRequest);
        UserResponseDto createdUser = userService.register(request, clientIp);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }
}
