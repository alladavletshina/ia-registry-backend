package com.example.reportservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private boolean active;
    private String firstName;
    private String lastName;
}