package com.example.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Управление задачами")
public class TaskController {

    @Operation(summary = "Получить статистику по задачам текущего пользователя (для админа — общую)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика")
    })
    @GetMapping("/stats")
    public String getStats() {
        return "TO BE code";
    }
}
