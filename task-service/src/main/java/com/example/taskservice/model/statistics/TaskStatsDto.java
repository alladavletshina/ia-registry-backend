package com.example.taskservice.model.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Статистика по задачам")
public class TaskStatsDto {
    @Schema(description = "Всего задач", example = "12")
    private long total;

    @Schema(description = "Ожидающих", example = "3")
    private long pending;

    @Schema(description = "В работе", example = "4")
    private long inProgress;

    @Schema(description = "Выполненных", example = "4")
    private long completed;

    @Schema(description = "Просроченных", example = "1")
    private long overdue;
}
