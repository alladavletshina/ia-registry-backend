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
                Map.of("service", "Auth Service", "path", "/api/auth/**", "url", "/auth-swagger-ui/"),
                Map.of("service", "Asset Service", "path", "/api/assets/**", "url", "/asset-swagger-ui/"),
                Map.of("service", "Auth API Docs", "path", "/auth-api-docs", "url", "/auth-api-docs"),
                Map.of("service", "Asset API Docs", "path", "/asset-api-docs", "url", "/asset-api-docs")
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
                "health", "http://localhost:8082/api/gateway/health"
        ));

        // Проверяем доступность других сервисов
        services.put("auth", checkService("Auth Service", "http://localhost:8083", "http://localhost:8082/auth-api-docs"));
        services.put("asset", checkService("Asset Service", "http://localhost:8084", "http://localhost:8082/asset-api-docs"));

        return ResponseEntity.ok(services);
    }

    private Map<String, Object> checkService(String name, String url, String gatewayDocsUrl) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("name", name);
        service.put("url", url);
        service.put("gatewayDocs", gatewayDocsUrl);
        service.put("swagger", url + "/swagger-ui.html");

        // Простая проверка доступности
        try {
            // В реальном приложении можно использовать WebClient для проверки
            service.put("status", "UNKNOWN");
            service.put("message", "Use gateway endpoints for access");
        } catch (Exception e) {
            service.put("status", "ERROR");
            service.put("message", e.getMessage());
        }

        return service;
    }

    @GetMapping("/swagger-urls")
    public ResponseEntity<?> getSwaggerUrls() {
        return ResponseEntity.ok(Map.of(
                "gateway", Map.of(
                        "swagger-ui", "http://localhost:8082/swagger-ui.html",
                        "api-docs", "http://localhost:8082/api-docs"
                ),
                "auth", Map.of(
                        "swagger-ui", "http://localhost:8082/auth-swagger-ui/",
                        "api-docs", "http://localhost:8082/auth-api-docs",
                        "direct", "http://localhost:8083/swagger-ui.html"
                ),
                "asset", Map.of(
                        "swagger-ui", "http://localhost:8082/asset-swagger-ui/",
                        "api-docs", "http://localhost:8082/asset-api-docs",
                        "direct", "http://localhost:8084/swagger-ui.html"
                ),
                "aggregated", true,
                "timestamp", System.currentTimeMillis()
        ));
    }
}