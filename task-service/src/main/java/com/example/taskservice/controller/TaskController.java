package com.example.taskservice.controller;

import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import com.example.taskservice.model.request.TaskCreateDto;
import com.example.taskservice.model.request.TaskUpdateDto;
import com.example.taskservice.model.response.TaskDto;
import com.example.taskservice.model.statistics.TaskStatsDto;
import com.example.taskservice.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Управление задачами")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Получить статистику по задачам текущего пользователя (для админа — общую)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика")
    })
    @GetMapping("/stats")
    public ResponseEntity<TaskStatsDto> getStats(@AuthenticationPrincipal Jwt jwt) {
        TaskStatsDto stats = taskService.getStats(jwt);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Получить заявку по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача найдена"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content),
            @ApiResponse(responseCode = "403", description = "Нет доступа к этой задаче", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskByid(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt
            ) {

        TaskDto task = taskService.getTaskById(id, jwt);
        return ResponseEntity.ok(task);

    }

    @Operation(summary = "Создать новую задачу (только для админа)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<TaskDto> createTask (
            @Valid @RequestBody TaskCreateDto createDto,
            @AuthenticationPrincipal Jwt jwt
            ) {
                TaskDto created = taskService.createTask(createDto, jwt);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Удалить задачу (только для админа)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Задача удалена"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена"),
            @ApiResponse(responseCode = "403", description = "Нет прав")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "ID задачи", required = true) @PathVariable long id
    ) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Частично обновить задачу (например, изменить статус)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача обновлена"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена"),
            @ApiResponse(responseCode = "403", description = "Нет прав")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask (
            @Parameter(description = "ID задачи", required = true) @PathVariable long id,
            @RequestBody TaskUpdateDto patchDto,
            @AuthenticationPrincipal Jwt jwt
            ) {
              TaskDto updated = taskService.updateTask(id,patchDto, jwt);
              return ResponseEntity.ok(updated);
    }


    @Operation(summary = "Получить список задач с фильтрацией и пагинацией",
            description = "Возвращает страницу задач, отфильтрованных по параметрам. Для обычного пользователя видны только его задачи.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<TaskDto>> getTasks(
            @Parameter(description = "Статус задачи") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Приоритет") @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Тип задачи") @RequestParam(required = false) TaskType type,
            @Parameter(description = "Поиск по названию, описанию, тегам") @RequestParam(required = false) String search,
            @Parameter(description = "ID актива") @RequestParam(required = false) Long assetId,
            @Parameter(description = "ID исполнителя (только для админа)") @RequestParam(required = false) UUID assignedTo,
            @Parameter(description = "Срок выполнения от") @RequestParam(required = false) LocalDate dueDateFrom,
            @Parameter(description = "Срок выполнения до") @RequestParam(required = false) LocalDate dueDateTo,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable,
            @Parameter(description = "ИД клиента") @RequestParam(required = false) UUID userId,
            @AuthenticationPrincipal Jwt jwt
            ) {
                Page<TaskDto> tasks = taskService.findTasks(status, priority, type,
                        search, assetId, assignedTo, dueDateFrom,
                        dueDateTo, pageable, jwt, userId);
                return ResponseEntity.ok(tasks);
    }
}
