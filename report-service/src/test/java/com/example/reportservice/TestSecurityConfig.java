package com.example.reportservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        // Возвращаем мок-токен при любом decode
        when(decoder.decode(anyString())).thenReturn(mockJwt());
        return decoder;
    }

    @Bean
    @Primary
    public Jwt mockJwt() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-user");
        claims.put("preferred_username", "admin");
        claims.put("realm_access", Map.of("roles", List.of("admin")));
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }
}