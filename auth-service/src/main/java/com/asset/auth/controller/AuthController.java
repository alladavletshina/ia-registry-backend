package com.asset.auth.controller;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Value("${keycloak.url:http://localhost:8080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:asset-management}")
    private String realm;

    @Value("${keycloak.client-id:asset-backend}")
    private String clientId;

    @Value("${keycloak.client-secret:backend-secret}")
    private String clientSecret;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.username);

        try {
            // Простая тестовая реализация без Keycloak
            if ("admin".equals(request.username) && "admin123".equals(request.password)) {
                Map<String, Object> response = new HashMap<>();
                response.put("accessToken", "test-jwt-token-" + System.currentTimeMillis());
                response.put("refreshToken", "test-refresh-token-" + System.currentTimeMillis());
                response.put("expiresIn", 3600);
                response.put("refreshExpiresIn", 7200);
                response.put("tokenType", "Bearer");
                response.put("scope", "openid profile email");
                response.put("message", "Authentication successful (TEST MODE)");

                log.info("User {} logged in successfully (TEST MODE)", request.username);
                return ResponseEntity.ok(response);
            } else if ("user".equals(request.username) && "user123".equals(request.password)) {
                Map<String, Object> response = new HashMap<>();
                response.put("accessToken", "test-user-token-" + System.currentTimeMillis());
                response.put("refreshToken", "test-user-refresh-" + System.currentTimeMillis());
                response.put("expiresIn", 3600);
                response.put("refreshExpiresIn", 7200);
                response.put("tokenType", "Bearer");
                response.put("scope", "openid profile email");
                response.put("message", "Authentication successful (TEST MODE)");

                log.info("User {} logged in successfully (TEST MODE)", request.username);
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials",
                            "message", "Use admin/admin123 or user/user123 for testing"));

        } catch (Exception e) {
            log.error("Login failed for user {}: {}", request.username, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Invalid credentials",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");

        try {
            // Простая тестовая реализация
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", "refreshed-token-" + System.currentTimeMillis());
            response.put("refreshToken", "new-refresh-token-" + System.currentTimeMillis());
            response.put("expiresIn", 3600);
            response.put("message", "Token refreshed successfully (TEST MODE)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Logout request");
        return ResponseEntity.ok(Map.of("message", "Logout successful (TEST MODE)"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Get current user request");

        try {
            // Простая тестовая реализация
            Map<String, Object> userInfo = new HashMap<>();

            if (authHeader != null && authHeader.contains("user-token")) {
                userInfo.put("username", "user");
                userInfo.put("email", "user@example.com");
                userInfo.put("firstName", "Test");
                userInfo.put("lastName", "User");
                userInfo.put("roles", new String[]{"user"});
            } else {
                userInfo.put("username", "admin");
                userInfo.put("email", "admin@example.com");
                userInfo.put("firstName", "Admin");
                userInfo.put("lastName", "User");
                userInfo.put("roles", new String[]{"admin", "user"});
            }

            userInfo.put("exp", System.currentTimeMillis() / 1000 + 3600);

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("Failed to get user info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth-service",
                "timestamp", System.currentTimeMillis(),
                "mode", "TEST"
        ));
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of(
                "message", "pong",
                "timestamp", System.currentTimeMillis(),
                "service", "auth-service"
        ));
    }

    // DTO классы без Lombok
    public static class LoginRequest {
        public String username;
        public String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        public String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class RegisterRequest {
        public String username;
        public String email;
        public String firstName;
        public String lastName;
        public String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}