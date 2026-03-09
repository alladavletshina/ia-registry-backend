package com.example.reportservice.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PerformanceReportDTO {
    private List<HourValue> responseTimes;
    private double uptime;
    private int avgResponseTime;
    private int peakLoad;
    private Map<String, Integer> errors;

    @Data
    public static class HourValue {
        private String hour;
        private int value;
    }
}