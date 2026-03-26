package com.example.assetservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddThreatRequest {
    private Long threatId;
    private BigDecimal probability;
    private Boolean customC;
    private Boolean customI;
    private Boolean customA;
    private BigDecimal mitigationEffect;
}