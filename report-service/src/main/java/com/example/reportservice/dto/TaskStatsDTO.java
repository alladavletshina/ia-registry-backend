package com.example.reportservice.dto;

import lombok.Data;

@Data
public class TaskStatsDTO {
    private long total;
    private long pending;
    private long inProgress;
    private long completed;
    private long overdue;
}