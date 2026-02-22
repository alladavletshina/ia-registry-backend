package com.example.userservice.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserRequestDto {
    private UUID id;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String position;
    private String department;
    private Boolean active;
//    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
