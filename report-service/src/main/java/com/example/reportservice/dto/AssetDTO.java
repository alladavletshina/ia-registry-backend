package com.example.reportservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssetDTO {
    private Long id;
    private String name;
    private String ownerId;
    private String status;
    private String confidentiality;
    private String integrity;
    private String availability;
    private String lastReview;
    private LocalDateTime createdAt;
    private String groupName;
    private String legalStatus;
}