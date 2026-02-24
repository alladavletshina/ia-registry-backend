package com.example.taskservice.model.request;

import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Данные для создания задачи")
public class TaskCreateDto {

    @NotBlank
    @Schema(description = "Название задачи", example = "Обновить БД клиентов", required = true)
    private String title;

    @Schema(description = "Описание", example = "Требуется обновить контактные данные...")
    private String description;

    @NotNull
    @Schema(description = "Приоритет", example = "HIGH", allowableValues = {"HIGH","MEDIUM","LOW"}, required = true)
    private TaskPriority priority;

    @NotNull
    @Schema(description = "Тип задачи", example = "UPDATE", allowableValues = {"UPDATE","REVIEW","REPORT","INVENTORY","BACKUP"}, required = true)
    private TaskType type;

    @FutureOrPresent
    @Schema(description = "Срок выполнения (не может быть в прошлом)", example = "2024-02-15")
    private LocalDate dueDate;

    @Schema(description = "Оценка времени", example = "4 часа")
    private String estimatedTime;

    @Schema(description = "Теги", example = "[\"обновление\",\"клиенты\"]")
    private List<String> tags;

    @Schema(description = "ID актива (если задача связана с активом)", example = "1")
    private Long assetId;

    @Schema(description = "ID пользователя, которому назначается задача (если не указан, назначается текущему)", example = "2")
    private UUID assignedTo;
}
