package com.example.reportservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class OverviewReportDTO {
    private long totalAssets;
    private long totalUsers;
    private long pendingReviews;
    private long highRiskAssets;
    private List<CategoryCount> categoryDistribution;
    private List<CiaAvg> ciaDistribution;

    @Data
    public static class CategoryCount {
        private String name;
        private int value;
        private String color;
    }

    @Data
    public static class CiaAvg {
        private String name;
        private double value;
        private String color;
    }
}