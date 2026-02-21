package com.example.assetservice.dto;

import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetResponse {

    private long id;

    public String name;
    private String category;
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

}
