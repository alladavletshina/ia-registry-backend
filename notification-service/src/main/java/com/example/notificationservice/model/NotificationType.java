package com.example.notificationservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    WARNING("Предупреждение"),
    SUCCESS("Успех"),
    INFO("Информация"),
    ASSIGNMENT("Назначение"),
    SECURITY("Безопасность");

    private final String description;
}
