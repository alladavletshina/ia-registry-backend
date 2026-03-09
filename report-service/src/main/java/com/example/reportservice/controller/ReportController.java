package com.example.reportservice.controller;

import com.example.reportservice.client.AssetServiceClient;
import com.example.reportservice.client.AuditServiceClient;
import com.example.reportservice.client.TaskServiceClient;
import com.example.reportservice.client.UserServiceClient;
import com.example.reportservice.dto.*;
import com.example.reportservice.service.ReportService;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Отчёты", description = "Формирование аналитических отчётов")
public class ReportController {

    private final ReportService reportService;

    private final UserServiceClient userClient;
    private final AssetServiceClient assetClient;
    private final TaskServiceClient taskClient;
    private final AuditServiceClient auditClient;

    @GetMapping("/overview")
    @Operation(summary = "Обзорная статистика")
    public OverviewReportDTO getOverview(
            @RequestParam(defaultValue = "month") String period) {
        return reportService.getOverviewReport(period);
    }

    @GetMapping("/assets")
    @Operation(summary = "Отчёт по активам")
    public AssetsReportDTO getAssets(
            @RequestParam(defaultValue = "month") String period) {
        return reportService.getAssetsReport(period);
    }

    @GetMapping("/users")
    @Operation(summary = "Активность пользователей")
    public UsersReportDTO getUsers(
            @RequestParam(defaultValue = "month") String period) {
        return reportService.getUsersReport(period);
    }

    @GetMapping("/security")
    @Operation(summary = "Отчёт по безопасности")
    public SecurityReportDTO getSecurity(
            @RequestParam(defaultValue = "month") String period) {
        return reportService.getSecurityReport(period);
    }

    @GetMapping("/performance")
    @Operation(summary = "Производительность системы")
    public PerformanceReportDTO getPerformance(
            @RequestParam(defaultValue = "month") String period) {
        return reportService.getPerformanceReport(period);
    }

    @GetMapping("/check-services")
    public ResponseEntity<Map<String, Object>> checkServices(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();

        // Информация о текущем пользователе
        if (jwt != null) {
            result.put("authenticated", true);
            result.put("username", jwt.getClaim("preferred_username"));
            result.put("subject", jwt.getSubject());
            result.put("roles", jwt.getClaim("realm_access"));
            result.put("token", jwt.getTokenValue().substring(0, 20) + "...");
        } else {
            result.put("authenticated", false);
        }

        // Проверка каждого сервиса
        result.put("services", checkAllServices());

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> checkAllServices() {
        Map<String, Object> services = new HashMap<>();

        // Проверка user-service
        try {
            var users = userClient.getAllUsers();
            services.put("user-service", Map.of(
                    "status", "UP",
                    "response", users != null ? "получено " + users.size() + " пользователей" : "OK"
            ));
        } catch (FeignException e) {
            services.put("user-service", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "statusCode", e.status()
            ));
        } catch (Exception e) {
            services.put("user-service", Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
        }

        // Проверка asset-service
        try {
            var assets = assetClient.getAllAssets();
            services.put("asset-service", Map.of(
                    "status", "UP",
                    "response", assets != null ? "получено " + assets.size() + " активов" : "OK"
            ));
        } catch (FeignException e) {
            services.put("asset-service", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "statusCode", e.status()
            ));
        } catch (Exception e) {
            services.put("asset-service", Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
        }

        // Проверка task-service (статистика)
        try {
            var stats = taskClient.getTaskStats();
            services.put("task-service", Map.of(
                    "status", "UP",
                    "response", stats != null ? "статистика получена" : "OK"
            ));
        } catch (FeignException e) {
            services.put("task-service", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "statusCode", e.status()
            ));
        } catch (Exception e) {
            services.put("task-service", Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
        }

        // Проверка audit-service (статистика)
        try {
            var stats = auditClient.getAuditEvents("2025-01-01", "2026-09-11");
            services.put("audit-service", Map.of(
                    "status", "UP",
                    "response", stats != null ? "статистика получена" : "OK"
            ));
        } catch (FeignException e) {
            services.put("audit-service", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "statusCode", e.status()
            ));
        } catch (Exception e) {
            services.put("audit-service", Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
        }

        return services;
    }
}