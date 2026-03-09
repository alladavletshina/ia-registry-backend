package com.example.reportservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AssetDTO {
    private Long id;
    private String name;
    private String category;
    private String owner;
    private String status;
    private String confidentiality;
    private String integrity;
    private String availability;
    private LocalDate lastReview;
    private LocalDate createdAt;
}