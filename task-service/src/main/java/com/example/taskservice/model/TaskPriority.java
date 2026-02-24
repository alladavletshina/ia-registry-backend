package com.example.taskservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskPriority {

    HIGH("Высокий"),
    MEDIUM("Средний"),
    LOW("Низкий");

    private final String description;
}
