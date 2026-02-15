package com.example.assetservice.dto;

import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAssetRequest {

    @NotBlank
    public String name;
    private String category;
    private String ownerId;

    @NotNull
    private AssetStatus status;

    @NotNull
    private CIA confidentiality;

    @NotNull
    private CIA integrity;

    @NotNull
    private CIA availability;

    private String lastReview;
    private String description;
    private String location;
    private String tags;
}
