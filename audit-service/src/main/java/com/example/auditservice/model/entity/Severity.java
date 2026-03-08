package com.example.auditservice.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Severity {
    INFO("Информация"),
    WARNING("Предупреждение"),
    DANGER("Опасность"),
    SUCCESS("Успех");

    private final String description;
}