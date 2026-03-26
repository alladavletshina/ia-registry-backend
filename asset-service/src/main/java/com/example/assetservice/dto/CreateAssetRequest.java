package com.example.assetservice.dto;

import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateAssetRequest {

    @NotBlank
    public String name;

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

    /*стоимость*/
    private BigDecimal value;

    /*вес конфиденциальности*/
    private Integer weightC = 1;

    /*вес целостности*/
    private Integer weightI = 1;

    /*вес доступности*/
    private Integer weightA = 1;

    /*правовой статус*/
    private String legalStatus;

    /*идентификатор группы активов*/
    private UUID groupId;
}
