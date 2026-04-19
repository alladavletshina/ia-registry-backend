package com.asset.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", appName);
        response.put("timestamp", System.currentTimeMillis());
        response.put("routes", List.of(
                Map.of("service", "Auth Service", "path", "/api/auth/**", "swagger", "/auth-swagger-ui/"),
                Map.of("service", "Asset Service", "path", "/api/assets/**", "swagger", "/asset-swagger-ui/"),
                Map.of("service", "User Service", "path", "/api/users/**", "swagger", "/user-swagger-ui/"),
                Map.of("service", "Task Service", "path", "/api/tasks/**", "swagger", "/task-swagger-ui/"),
                Map.of("service", "Notification Service", "path", "/api/notifications/**", "swagger", "/notification-swagger-ui/"),
                Map.of("service", "Audit Service", "path", "/api/audit/**", "swagger", "/audit-swagger-ui/"),
                Map.of("service", "Report Service", "path", "/api/reports/**", "swagger", "/report-swagger-ui/")
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/services")
    public ResponseEntity<?> getServices() {
        Map<String, Object> services = new LinkedHashMap<>();

        services.put("timestamp", System.currentTimeMillis());
        services.put("gateway", Map.of(
                "name", "API Gateway",
                "url", "http://localhost:8082",
                "swagger", "http://localhost:8082/swagger-ui.html",
                "api-docs", "http://localhost:8082/api-docs",
                "health", "http://localhost:8082/api/gateway/health",
                "dashboard", "http://localhost:8082/"
        ));

        services.put("asset", Map.of(
                "name", "Asset Service",
                "url", "http://localhost:8084",
                "gatewayDocs", "http://localhost:8082/asset-api-docs",
                "swagger", "http://localhost:8082/asset-swagger-ui/",
                "directSwagger", "http://localhost:8084/swagger-ui.html",
                "health", "http://localhost:8082/api/assets/health"
        ));

        services.put("user", Map.of(
                "name", "User Service",
                "url", "http://localhost:8085",
                "gatewayDocs", "http://localhost:8082/user-api-docs",
                "swagger", "http://localhost:8082/user-swagger-ui/",
                "directSwagger", "http://localhost:8085/swagger-ui.html",
                "health", "http://localhost:8082/api/users/health"
        ));

        services.put("task", Map.of(
                "name", "Task Service",
                "url", "http://localhost:8086",
                "gatewayDocs", "http://localhost:8082/task-api-docs",
                "swagger", "http://localhost:8082/task-swagger-ui/",
                "directSwagger", "http://localhost:8086/swagger-ui.html",
                "health", "http://localhost:8082/api/tasks/health"
        ));

        services.put("notification", Map.of(
                "name", "Notification Service",
                "url", "http://localhost:8087",
                "gatewayDocs", "http://localhost:8082/notification-api-docs",
                "swagger", "http://localhost:8082/notification-swagger-ui/",
                "directSwagger", "http://localhost:8087/swagger-ui.html",
                "health", "http://localhost:8082/api/notifications/health"
        ));

        services.put("audit", Map.of(
                "name", "Audit Service",
                "url", "http://localhost:8088",
                "gatewayDocs", "http://localhost:8082/audit-api-docs",
                "swagger", "http://localhost:8082/audit-swagger-ui/",
                "directSwagger", "http://localhost:8088/swagger-ui.html",
                "health", "http://localhost:8082/api/audit/health"
        ));

        services.put("report", Map.of(
                "name", "Report Service",
                "url", "http://localhost:8089",
                "gatewayDocs", "http://localhost:8082/report-api-docs",
                "swagger", "http://localhost:8082/report-swagger-ui/",
                "directSwagger", "http://localhost:8089/swagger-ui.html",
                "health", "http://localhost:8082/api/reports/health"
        ));

        services.put("keycloak", Map.of(
                "name", "Keycloak",
                "url", "http://localhost:8080",
                "admin", "http://localhost:8080/admin",
                "credentials", "admin / admin123"
        ));

        return ResponseEntity.ok(services);
    }

    @GetMapping("/swagger-urls")
    public ResponseEntity<?> getSwaggerUrls() {
        return ResponseEntity.ok(Map.of(
                "gateway", Map.of(
                        "swagger-ui", "http://localhost:8082/swagger-ui.html",
                        "api-docs", "http://localhost:8082/api-docs",
                        "dashboard", "http://localhost:8082/"
                ),
                "asset", Map.of(
                        "swagger-ui", "http://localhost:8082/asset-swagger-ui/",
                        "api-docs", "http://localhost:8082/asset-api-docs",
                        "direct", "http://localhost:8084/swagger-ui.html"
                ),
                "user", Map.of(
                        "swagger-ui", "http://localhost:8082/user-swagger-ui/",
                        "api-docs", "http://localhost:8082/user-api-docs",
                        "direct", "http://localhost:8085/swagger-ui.html"
                ),
                "task", Map.of(
                        "swagger-ui", "http://localhost:8082/task-swagger-ui/",
                        "api-docs", "http://localhost:8082/task-api-docs",
                        "direct", "http://localhost:8086/swagger-ui.html"
                ),
                "notification", Map.of(
                        "swagger-ui", "http://localhost:8082/notification-swagger-ui/",
                        "api-docs", "http://localhost:8082/notification-api-docs",
                        "direct", "http://localhost:8087/swagger-ui.html"
                ),
                "audit", Map.of(
                        "swagger-ui", "http://localhost:8082/audit-swagger-ui/",
                        "api-docs", "http://localhost:8082/audit-api-docs",
                        "direct", "http://localhost:8088/swagger-ui.html"
                ),
                "report", Map.of(
                        "swagger-ui", "http://localhost:8082/report-swagger-ui/",
                        "api-docs", "http://localhost:8082/report-api-docs",
                        "direct", "http://localhost:8089/swagger-ui.html"
                ),
                "timestamp", System.currentTimeMillis()
        ));
    }
}