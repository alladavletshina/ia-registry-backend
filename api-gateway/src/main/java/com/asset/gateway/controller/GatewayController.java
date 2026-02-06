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
                "health", "http://localhost:8082/api/gateway/health",
                "dashboard", "http://localhost:8082/"
        ));

        services.put("auth", Map.of(
                "name", "Auth Service",
                "url", "http://localhost:8083",
                "gatewayDocs", "http://localhost:8082/auth-api-docs",
                "swagger", "http://localhost:8082/auth-swagger-ui/",
                "directSwagger", "http://localhost:8083/swagger-ui.html",
                "health", "http://localhost:8082/api/auth/health"
        ));

        services.put("asset", Map.of(
                "name", "Asset Service",
                "url", "http://localhost:8084",
                "gatewayDocs", "http://localhost:8082/asset-api-docs",
                "swagger", "http://localhost:8082/asset-swagger-ui/",
                "directSwagger", "http://localhost:8084/swagger-ui.html",
                "health", "http://localhost:8082/api/assets/health"
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
                "timestamp", System.currentTimeMillis()
        ));
    }
}