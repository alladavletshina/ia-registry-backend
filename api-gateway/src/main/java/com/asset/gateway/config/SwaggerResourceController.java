package com.asset.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerResourceController {

    @Bean
    public WebFilter swaggerWebFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Если запрос к Swagger UI, добавляем правильные URL
            if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-Swagger-Resources", getSwaggerResources())
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            return chain.filter(exchange);
        };
    }

    private String getSwaggerResources() {
        return String.join(",",
                "API Gateway:/v3/api-docs",
                "Auth Service:/auth-api-docs/v3/api-docs",
                "Asset Service:/asset-api-docs/v3/api-docs"
        );
    }
}