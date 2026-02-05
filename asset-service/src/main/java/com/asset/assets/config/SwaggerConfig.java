// ./asset-service/src/main/java/com/asset/assets/config/SwaggerConfig.java
package com.asset.assets.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Asset Service API",
                version = "1.0.0",
                description = """
                        ## Сервис управления активами
                        
                        ### Права доступа:
                        - **Публичные endpoints**: health, test, public-info, db-check
                        - **Роль USER**: просмотр всех активов, просмотр своих активов
                        - **Роль ADMIN**: все операции + создание активов
                        
                        ### Модель данных актива:
                        - id, name, description, category
                        - owner, status, confidentiality, integrity, availability
                        - createdAt, updatedAt, lastReview
                        """,
                contact = @Contact(
                        name = "Asset Service Team",
                        email = "assets@asset-management.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://springdoc.org"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8084", description = "Local Development"),
                @Server(url = "http://asset-service:8084", description = "Docker Internal")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
}