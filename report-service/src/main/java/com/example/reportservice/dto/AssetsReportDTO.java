package com.example.reportservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AssetsReportDTO {
    private List<CategoryCount> byCategory;
    private List<StatusCount> byStatus;
    private List<LevelCount> byConfidentiality;
    private List<MonthValue> growthTrend;
    private BigDecimal totalRisk;

    @Data
    public static class CategoryCount {
        private String name;
        private int value;
        private String color;
    }

    @Data
    public static class StatusCount {
        private String name;
        private int value;
        private String color;
    }

    @Data
    public static class LevelCount {
        private String name;
        private int value;
        private String color;
    }

    @Data
    public static class MonthValue {
        private String month;
        private int value;
    }
}