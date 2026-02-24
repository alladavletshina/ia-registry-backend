package com.example.taskservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    PENDING("Ожидает"),
    IN_PROGRESS("В работе"),
    COMPLETED("Выполнена"),
    OVERDUE("Просрочена");

    private final String description;
}
