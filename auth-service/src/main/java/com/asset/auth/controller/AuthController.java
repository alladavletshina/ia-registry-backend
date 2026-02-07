package com.asset.auth.controller;

import org.springframework.beans.factory.annotation.Value; // ВАЖНО: Добавить этот импорт
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://localhost:8082"})
public class AuthController {

    // Эти аннотации требуют импорта org.springframework.beans.factory.annotation.Value
    @Value("${keycloak.url:http://localhost:8180}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm:asset-management}")
    private String realm;

    @Value("${keycloak.client-id:asset-backend}")
    private String clientId;

    @Value("${keycloak.client-secret:backend-secret}")
    private String clientSecret;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Value("${auth.fallback.enabled:false}")
    private boolean fallbackEnabled;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        System.out.println("🚀 LOGIN STARTED for user: " + credentials.get("username"));

        String username = credentials.get("username");
        String password = credentials.get("password");

        // Проверка тестовых учетных данных
        boolean isTestAdmin = "admin".equals(username) && "admin123".equals(password);
        boolean isTestUser = "user".equals(username) && "user123".equals(password);

        if (!isTestAdmin && !isTestUser) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        // Пробуем получить токен от Keycloak
        if (keycloakEnabled) {
            try {
                String url = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
                System.out.println("📞 Calling Keycloak: " + url);

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("client_id", clientId);
                params.add("client_secret", clientSecret);
                params.add("username", username);
                params.add("password", password);
                params.add("grant_type", "password");
                params.add("scope", "openid");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
                RestTemplate restTemplate = new RestTemplate();

                ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.POST, request, Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> tokenData = response.getBody();
                    System.out.println("✅ SUCCESS! Got token from Keycloak");

                    return buildSuccessResponse(tokenData, "SUCCESS - Real JWT token from Keycloak");
                } else {
                    System.out.println("❌ Keycloak returned error: " + response.getStatusCode());
                }
            } catch (Exception e) {
                System.out.println("❌ Keycloak error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Fallback режим
        if (fallbackEnabled) {
            System.out.println("⚠️ Using fallback test mode");
            return buildFallbackResponse(username);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Authentication service unavailable"));
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(
            Map<String, Object> tokenData, String message) {

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", tokenData.get("access_token"));
        result.put("refreshToken", tokenData.get("refresh_token"));
        result.put("expiresIn", tokenData.get("expires_in"));
        result.put("tokenType", tokenData.get("token_type"));
        result.put("scope", tokenData.get("scope"));
        result.put("message", message);

        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", "test-jwt-token-" + username + "-" + System.currentTimeMillis());
        result.put("refreshToken", "test-refresh-token-" + username + "-" + System.currentTimeMillis());
        result.put("expiresIn", 3600);
        result.put("refreshExpiresIn", 7200);
        result.put("tokenType", "Bearer");
        result.put("scope", "openid profile email");
        result.put("message", "Authentication successful (TEST MODE)");
        result.put("warning", "Using test mode because Keycloak is not accessible");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("👤 GET CURRENT USER request");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No token provided"));
        }

        try {
            String token = authHeader.substring(7);
            System.out.println("Token received, length: " + token.length());

            // Проверяем, это тестовый токен или реальный JWT
            boolean isTestToken = token.startsWith("test-jwt-token-");
            boolean isRealJWT = token.split("\\.").length == 3;

            if (isTestToken) {
                System.out.println("Using test token logic");
                // Тестовый токен - возвращаем тестовые данные
                Map<String, Object> userInfo = new HashMap<>();

                if (token.contains("admin")) {
                    userInfo.put("username", "admin");
                    userInfo.put("email", "admin@example.com");
                    userInfo.put("firstName", "Admin");
                    userInfo.put("lastName", "User");
                    userInfo.put("roles", new String[]{"admin", "user"});
                } else if (token.contains("user")) {
                    userInfo.put("username", "user");
                    userInfo.put("email", "user@example.com");
                    userInfo.put("firstName", "Test");
                    userInfo.put("lastName", "User");
                    userInfo.put("roles", new String[]{"user"});
                } else {
                    userInfo.put("username", "unknown");
                    userInfo.put("roles", new String[]{"guest"});
                }

                userInfo.put("tokenType", "Bearer");
                userInfo.put("active", true);
                userInfo.put("exp", System.currentTimeMillis() / 1000 + 3600);
                userInfo.put("mode", "TEST");

                return ResponseEntity.ok(userInfo);

            } else if (isRealJWT) {
                System.out.println("Using real JWT token");

                try {
                    // Пробуем интроспекцию токена через Keycloak
                    String introspectUrl = "http://keycloak:8080/realms/asset-management/protocol/openid-connect/token/introspect";

                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("client_id", "asset-backend");
                    params.add("client_secret", "backend-secret");
                    params.add("token", token);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

                    RestTemplate restTemplate = new RestTemplate();
                    ResponseEntity<Map> response = restTemplate.postForEntity(introspectUrl, request, Map.class);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        Map<String, Object> introspectResponse = response.getBody();

                        boolean active = Boolean.TRUE.equals(introspectResponse.get("active"));
                        if (active) {
                            System.out.println("✅ Token is active");

                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("username", introspectResponse.get("preferred_username"));
                            userInfo.put("email", introspectResponse.get("email"));
                            userInfo.put("firstName", introspectResponse.get("given_name"));
                            userInfo.put("lastName", introspectResponse.get("family_name"));
                            userInfo.put("active", true);
                            userInfo.put("exp", introspectResponse.get("exp"));
                            userInfo.put("iat", introspectResponse.get("iat"));
                            userInfo.put("iss", introspectResponse.get("iss"));
                            userInfo.put("aud", introspectResponse.get("aud"));
                            userInfo.put("mode", "PRODUCTION");

                            return ResponseEntity.ok(userInfo);
                        } else {
                            System.out.println("❌ Token is not active");
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                    .body(Map.of("error", "Token is not active"));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Token introspection failed: " + e.getMessage());

                    // Fallback: декодируем JWT самостоятельно (только payload)
                    try {
                        String[] parts = token.split("\\.");
                        if (parts.length >= 2) {
                            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                            Map<String, Object> payloadMap = new HashMap<>();

                            // Простой парсинг JSON
                            String json = payload.replace("{", "").replace("}", "");
                            String[] pairs = json.split(",");
                            for (String pair : pairs) {
                                String[] keyValue = pair.split(":");
                                if (keyValue.length == 2) {
                                    String key = keyValue[0].trim().replace("\"", "");
                                    String value = keyValue[1].trim().replace("\"", "");
                                    payloadMap.put(key, value);
                                }
                            }

                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("username", payloadMap.get("preferred_username"));
                            userInfo.put("email", payloadMap.get("email"));
                            userInfo.put("active", true);
                            userInfo.put("mode", "PRODUCTION (decoded)");

                            return ResponseEntity.ok(userInfo);
                        }
                    } catch (Exception e2) {
                        System.out.println("❌ JWT decoding failed: " + e2.getMessage());
                    }
                }
            }

            // Если ничего не сработало
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token format"));

        } catch (Exception e) {
            System.out.println("❌ Error getting user info: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        System.out.println("🏥 Health check called");

        try {
            // Простая проверка Keycloak
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://keycloak:8080/realms/asset-management/.well-known/openid-configuration";
            restTemplate.getForObject(url, Map.class);

            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "auth-service",
                    "keycloak", "REACHABLE",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "auth-service",
                    "keycloak", "UNREACHABLE",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        System.out.println("🔄 REFRESH TOKEN request");

        if (!request.containsKey("refreshToken")) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken is required"));
        }

        String refreshToken = request.get("refreshToken");

        try {
            String url = "http://keycloak:8080/realms/asset-management/protocol/openid-connect/token";

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", "asset-backend");
            params.add("client_secret", "backend-secret");
            params.add("refresh_token", refreshToken);
            params.add("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenData = response.getBody();

                Map<String, Object> result = new HashMap<>();
                result.put("accessToken", tokenData.get("access_token"));
                result.put("refreshToken", tokenData.get("refresh_token"));
                result.put("expiresIn", tokenData.get("expires_in"));
                result.put("tokenType", tokenData.get("token_type"));
                result.put("message", "Token refreshed successfully");

                return ResponseEntity.ok(result);
            } else {
                System.out.println("❌ Keycloak refresh failed: " + response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode())
                        .body(response.getBody());
            }

        } catch (Exception e) {
            System.out.println("❌ Error refreshing token: " + e.getMessage());

            // Fallback для тестовых токенов
            if (refreshToken.startsWith("test-refresh-token-")) {
                Map<String, Object> result = new HashMap<>();
                result.put("accessToken", "refreshed-test-token-" + System.currentTimeMillis());
                result.put("refreshToken", "new-test-refresh-" + System.currentTimeMillis());
                result.put("expiresIn", 3600);
                result.put("tokenType", "Bearer");
                result.put("message", "Token refreshed (TEST MODE)");

                return ResponseEntity.ok(result);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh failed", "details", e.getMessage()));
        }
    }
}