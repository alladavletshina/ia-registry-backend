package com.example.assetservice.dto;

import com.example.assetservice.model.entity.AssetThreat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AssetThreatDto {
    private UUID id;
    private Long assetId;
    private Long threatId;
    private String threatName;
    private BigDecimal probability;
    private Boolean customC;
    private Boolean customI;
    private Boolean customA;
    private BigDecimal mitigationEffect;
    private String status;
    private LocalDate assessmentDate;

    public static AssetThreatDto fromEntity(AssetThreat at) {
        AssetThreatDto dto = new AssetThreatDto();
        dto.setId(at.getId());
        dto.setAssetId(at.getAsset().getId());
        dto.setThreatId(at.getThreat().getId());
        dto.setThreatName(at.getThreat().getName());
        dto.setProbability(at.getProbability());
        dto.setCustomC(at.getCustomC());
        dto.setCustomI(at.getCustomI());
        dto.setCustomA(at.getCustomA());
        dto.setMitigationEffect(at.getMitigationEffect());
        dto.setStatus(at.getStatus());
        dto.setAssessmentDate(at.getAssessmentDate());
        return dto;
    }
}
