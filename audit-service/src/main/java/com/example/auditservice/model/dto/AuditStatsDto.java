package com.example.auditservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class AuditStatsDto {
    private long total;
    private long info;
    private long warning;
    private long danger;
    private long success;
}
