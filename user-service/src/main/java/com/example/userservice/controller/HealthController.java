package com.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Health", description = "Проверка работоспособности сервиса")
public class HealthController {

    @Operation(summary = "Проверка здоровья", description = "Возвращает OK, если сервис работает")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @Operation(summary = "Проверка здоровья для Actuator", description = "Возвращает статус UP в JSON")
    @GetMapping("/actuator/health")
    public ResponseEntity<String> actuatorHealth() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }

    @Operation(hidden = true)
    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("Task Service is running");
    }
}
