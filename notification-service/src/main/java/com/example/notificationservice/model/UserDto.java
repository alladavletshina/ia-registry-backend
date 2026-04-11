package com.example.notificationservice.model;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;           // внутренний ID пользователя
    private String keycloakId; // Keycloak ID
    private String email;
}