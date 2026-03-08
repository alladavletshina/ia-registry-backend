package com.example.auditservice.model.dto;

import com.example.auditservice.model.entity.Severity;
import lombok.Data;

import java.util.UUID;

@Data
public class AuditEventDto {
    private UUID userId;
    private String username;
    private String action;
    private String details;
    private String ip;
    private Severity severity;
    private String serviceName;
    private String objectId;
    private String objectType;
}
