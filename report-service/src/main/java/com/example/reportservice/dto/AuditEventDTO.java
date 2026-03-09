package com.example.reportservice.dto;

import lombok.Data;

@Data
public class AuditEventDTO {
    private String timestamp;
    private String user;
    private String action;
    private String details;
    private String ip;
    private String severity;
}