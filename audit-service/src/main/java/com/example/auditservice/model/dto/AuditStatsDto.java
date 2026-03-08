package com.example.auditservice.model.dto;

import lombok.Data;

@Data
public class AuditStatsDto {
    private long total;
    private long info;
    private long warning;
    private long danger;
    private long success;
}
