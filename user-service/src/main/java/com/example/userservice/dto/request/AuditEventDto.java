package com.example.userservice.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class AuditEventDto {
    private UUID userId;
    private String username;
    private String action;
    private String details;
    private String ip;
    private String severity; // INFO, WARNING, DANGER, SUCCESS
    private String serviceName;
    private String objectId;
    private String objectType;
}
