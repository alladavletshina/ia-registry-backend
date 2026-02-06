package com.asset.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AggregatedSwaggerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @Primary
    public OpenAPI aggregatedOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Asset Management - Unified API Gateway")
                        .description("""
                            ## Unified API Gateway for all services
                            
                            ### Available services:
                            1. **API Gateway** - routing and gateway information
                            2. **Auth Service** (`/api/auth/**`) - authentication
                            3. **Asset Service** (`/api/assets/**`) - asset management
                            
                            ### How to use:
                            1. Open dropdown in top-right corner to select service
                            2. **Auth Service** - endpoints start with `/api/auth/`
                            3. **Asset Service** - endpoints start with `/api/assets/`
                            4. **Gateway** - endpoints start with `/api/gateway/`
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Asset Management Team")
                                .email("support@asset-management.com")
                                .url("http://asset-management.com"))
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
                .description("Local development (Gateway)"));

        servers.add(new Server()
                .url("http://auth-service:8083")
                .description("Auth Service (direct)"));

        servers.add(new Server()
                .url("http://asset-service:8084")
                .description("Asset Service (direct)"));

        return servers;
    }
}