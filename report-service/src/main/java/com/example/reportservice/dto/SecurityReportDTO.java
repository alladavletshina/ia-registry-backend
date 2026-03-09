package com.example.reportservice.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SecurityReportDTO {
    private List<RiskCount> riskDistribution;
    private List<DateCount> auditEvents;
    private Map<String, Integer> complianceStatus;

    @Data
    public static class RiskCount {
        private String name;
        private int value;
        private String color;
    }

    @Data
    public static class DateCount {
        private String date;
        private int value;
    }
}