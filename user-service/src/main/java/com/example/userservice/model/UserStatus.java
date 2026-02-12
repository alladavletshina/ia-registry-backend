package com.example.userservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {

    PENDING("Ожидает подтверждения"),
    ACTIVE("Активен"),
    BLOCKED("Заблокирован"),
    FAILED("Ошибка регистрации"),
    DELETED("Удален");

    private final String description;
}
