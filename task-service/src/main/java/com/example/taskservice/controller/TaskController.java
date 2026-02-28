package com.example.taskservice.controller;

import com.example.taskservice.model.request.TaskCreateDto;
import com.example.taskservice.model.request.TaskUpdateDto;
import com.example.taskservice.model.response.TaskDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
    public String getStats() {
        return "TO BE code";
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
}
