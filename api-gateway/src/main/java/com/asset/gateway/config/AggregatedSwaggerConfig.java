package com.asset.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AggregatedSwaggerConfig {

    @Bean
    @Primary
    public OpenAPI aggregatedOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Asset Management - Unified API Gateway")
                        .description("""
                            ## Unified API Gateway for all services
                            
                            ### Available services:
                            1. **API Gateway** (`/api/gateway/**`) - routing and gateway information
                            2. **Asset Service** (`/api/assets/**`) - asset management
                            3. **User Service** (`/api/users/**`) - user management
                            4. **Task Service** (`/api/tasks/**`) - task management
                            5. **Notification Service** (`/api/notifications/**`) - user notifications
                            6. **Audit Service** (`/api/audit/**`) - audit logs
                            7. **Report Service** (`/api/reports/**`) - analytics reports
                            
                            ### How to use:
                            1. Select a service from the dropdown in the top-right corner
                            2. All endpoints require a valid JWT token (except `/api/auth/login` and health checks)
                            3. Click "Authorize" button and enter your Bearer token
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Asset Management Team")
                                .email("support@asset-management.com")
                                .url("https://asset-management.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .servers(getServers());
    }

    private List<Server> getServers() {
        List<Server> servers = new ArrayList<>();

        servers.add(new Server()
                .url("http://localhost:8082")
                .description("API Gateway (local development)"));

        servers.add(new Server()
                .url("http://asset-service:8084")
                .description("Asset Service (direct)"));

        servers.add(new Server()
                .url("http://user-service:8085")
                .description("User Service (direct)"));

        servers.add(new Server()
                .url("http://task-service:8086")
                .description("Task Service (direct)"));

        servers.add(new Server()
                .url("http://notification-service:8087")
                .description("Notification Service (direct)"));

        servers.add(new Server()
                .url("http://audit-service:8088")
                .description("Audit Service (direct)"));

        servers.add(new Server()
                .url("http://report-service:8089")
                .description("Report Service (direct)"));

        return servers;
    }
}