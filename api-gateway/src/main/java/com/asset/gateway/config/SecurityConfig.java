package com.asset.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        // Swagger endpoints - разрешаем всем
                        .pathMatchers(
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/auth-swagger-ui/**",
                                "/auth-api-docs/**",
                                "/auth-webjars/**",
                                "/asset-swagger-ui/**",
                                "/asset-api-docs/**",
                                "/asset-webjars/**"
                        ).permitAll()

                        // Auth endpoints - разрешаем всем
                        .pathMatchers("/api/auth/**").permitAll()

                        // Health checks - разрешаем всем
                        .pathMatchers("/actuator/health", "/health", "/ping").permitAll()

                        // Asset endpoints - требуют аутентификацию
                        .pathMatchers("/api/assets/**").authenticated()

                        // Everything else
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwkSetUri(
                                "http://keycloak:8080/realms/asset-management/protocol/openid-connect/certs"))
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}