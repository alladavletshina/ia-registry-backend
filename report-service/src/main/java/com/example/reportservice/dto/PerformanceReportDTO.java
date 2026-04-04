package com.example.reportservice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PerformanceReportDTO {
    private double uptime;
    private int avgResponseTime;
    private Map<String, Integer> errors;
}