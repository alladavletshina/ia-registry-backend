package com.example.assetservice.dto;

import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssetResponse {

    private long id;

    public String name;
    private String ownerId;

    private AssetStatus status;

    private CIA confidentiality;
    private CIA integrity;
    private CIA availability;

    private String lastReview;
    private String description;
    private String location;
    private String tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BigDecimal value;
    private Integer weightC;
    private Integer weightI;
    private Integer weightA;
    private String legalStatus;
    private UUID groupId;

    private BigDecimal latestRisk;

    /*название группы*/
    private String groupName;
}
