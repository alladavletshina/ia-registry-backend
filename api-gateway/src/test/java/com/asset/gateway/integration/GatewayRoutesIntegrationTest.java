package com.asset.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Мокаем декодер JWT, чтобы не было реальных вызовов к Keycloak
    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Переопределяем URL Keycloak на несуществующий, чтобы не было попыток соединения
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:9999");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost:9999");
    }

    @Test
    void publicEndpointsShouldBeAccessible() {
        ResponseEntity<Map> healthResp = restTemplate.getForEntity(
                "/api/gateway/health", Map.class);
        assertThat(healthResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResp.getBody()).containsEntry("status", "UP");
    }

    @Test
    void protectedEndpointShouldReturnUnauthorized() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/assets", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void corsPreflightShouldSucceed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:3000");
        headers.set("Access-Control-Request-Method", "GET");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/assets",
                HttpMethod.OPTIONS,
                entity,
                Void.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getAccessControlAllowOrigin()).contains("http://localhost:3000");
    }
}