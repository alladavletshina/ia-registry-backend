package com.example.taskservice.model.response;

import lombok.Data;
import java.util.UUID;

@Data
public class NotificationCreateDto {
    private UUID keyclockId;      // кому адресовано (ID пользователя)
    private String type;          // WARNING, SUCCESS, INFO, ASSIGNMENT, SECURITY
    private String title;
    private String message;
    private String actionUrl;
    private String actionLabel;
}
