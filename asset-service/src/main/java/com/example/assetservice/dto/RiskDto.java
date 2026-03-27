package com.example.assetservice.dto;

import com.example.assetservice.model.entity.Risk;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RiskDto {
    private UUID id;
    private Long assetId;
    private BigDecimal calculatedRisk;
    private LocalDateTime calculationDate;
    private String calculationDetails;

    public static RiskDto fromEntity(Risk risk) {
        RiskDto dto = new RiskDto();
        dto.setId(risk.getId());
        dto.setAssetId(risk.getAsset().getId());
        dto.setCalculatedRisk(risk.getCalculatedRisk());
        dto.setCalculationDate(risk.getCalculationDate());
        dto.setCalculationDetails(risk.getCalculationDetails());
        return dto;
    }
}
