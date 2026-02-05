// ./api-gateway/src/main/java/com/asset/gateway/config/SwaggerConfig.java
package com.asset.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Asset Management API Gateway")
                        .description("""
                            ## API Gateway для Asset Management System
                            
                            ### Маршруты:
                            - **Auth Service**: `/api/auth/**`
                            - **Asset Service**: `/api/assets/**`
                            
                            ### Аутентификация:
                            Все запросы к Asset Service требуют JWT токен.
                            Получить токен можно через Auth Service.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Asset Management Team")
                                .email("support@asset-management.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://api.asset-management.com")
                                .description("Production Server")
                ));
    }
}