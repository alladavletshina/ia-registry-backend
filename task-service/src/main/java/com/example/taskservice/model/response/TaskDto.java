package com.example.taskservice.model.response;

import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Данные задачи")
public class TaskDto {
    @Schema(description = "ID задачи", example = "1")
    private Long id;

    @Schema(description = "Название", example = "Обновить БД клиентов")
    private String title;

    @Schema(description = "Описание", example = "Требуется обновить контактные данные...")
    private String description;

    @Schema(description = "Приоритет", example = "HIGH")
    private TaskPriority priority;

    @Schema(description = "Статус", example = "IN_PROGRESS")
    private TaskStatus status;

    @Schema(description = "Тип задачи", example = "UPDATE")
    private TaskType type;

    @Schema(description = "Срок выполнения", example = "2024-02-15")
    private LocalDate dueDate;

    @Schema(description = "Дата завершения", example = "2024-02-10")
    private LocalDate completedAt;

    @Schema(description = "Оценка времени", example = "4 часа")
    private String estimatedTime;

    @Schema(description = "Теги", example = "[\"обновление\",\"клиенты\"]")
    private List<String> tags;

    @Schema(description = "ID связанного актива", example = "1")
    private Long assetId;

    @Schema(description = "Название связанного актива", example = "База данных клиентов")
    private String assetName;

    @Schema(description = "ID пользователя, на которого назначена задача", example = "2")
    private UUID assignedTo;

    @Schema(description = "ID пользователя, создавшего задачу", example = "1")
    private UUID assignedBy;

    @Schema(description = "Дата создания", example = "2024-01-25")
    private LocalDate createdAt;

    @Schema(description = "Дата обновления", example = "2024-01-30")
    private LocalDate updatedAt;
}
