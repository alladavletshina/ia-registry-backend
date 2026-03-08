package com.example.auditservice.model.dto;

import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
public class AuditLogDto {
    private UUID id;
    private String timestamp;
    private String user;
    private String action;
    private String details;
    private String ip;
    private String severity;

    public static AuditLogDto fromEntity(com.example.auditservice.model.entity.AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setTimestamp(log.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dto.setUser(log.getUsername());
        dto.setAction(log.getAction());
        dto.setDetails(log.getDetails());
        dto.setIp(log.getIp());
        dto.setSeverity(log.getSeverity().name().toLowerCase());
        return dto;
    }
}
