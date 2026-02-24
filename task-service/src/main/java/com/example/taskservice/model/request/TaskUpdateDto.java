package com.example.taskservice.model.request;

import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Данные для частичного обновления задачи (все поля опциональны)")
public class TaskUpdateDto {
    @Schema(description = "Новое название")
    private String title;

    @Schema(description = "Новое описание")
    private String description;

    @Schema(description = "Новый приоритет", allowableValues = {"HIGH","MEDIUM","LOW"})
    private TaskPriority priority;

    @Schema(description = "Новый статус", allowableValues = {"PENDING","IN_PROGRESS","COMPLETED","OVERDUE"})
    private TaskStatus status;

    @Schema(description = "Новый тип")
    private TaskType type;

    @Schema(description = "Новый срок выполнения")
    private LocalDate dueDate;

    @Schema(description = "Новая оценка времени")
    private String estimatedTime;

    @Schema(description = "Новые теги")
    private List<String> tags;

    @Schema(description = "Новый ID актива")
    private Long assetId;

    @Schema(description = "Новое имя актива (если меняется)")
    private String assetName;

    @Schema(description = "Новый исполнитель")
    private UUID assignedTo;
}