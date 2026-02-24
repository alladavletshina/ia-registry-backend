package com.example.taskservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskType {
    UPDATE("Обновление"),
    REVIEW("Проверка"),
    REPORT("Отчет"),
    INVENTORY("Инвентаризация"),
    BACKUP("Резервное копирование");

    private final String description;
}
