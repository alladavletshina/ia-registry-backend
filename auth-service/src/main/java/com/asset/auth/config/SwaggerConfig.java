package com.asset.auth.config;

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
                title = "Auth Service API",
                version = "1.0.0",
                description = """
                        ## Сервис аутентификации и авторизации
                        
                        ### Тестовые пользователи:
                        - **admin** / **admin123** - администратор
                        - **user** / **user123** - обычный пользователь
                        
                        ### Endpoints:
                        - `POST /api/auth/login` - вход в систему
                        - `POST /api/auth/refresh` - обновление токена
                        - `GET /api/auth/me` - информация о текущем пользователе
                        - `POST /api/auth/logout` - выход из системы
                        """,
                contact = @Contact(
                        name = "Auth Service Team",
                        email = "auth@asset-management.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://springdoc.org"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8083", description = "Local Development"),
                @Server(url = "http://auth-service:8082", description = "Docker Internal")
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