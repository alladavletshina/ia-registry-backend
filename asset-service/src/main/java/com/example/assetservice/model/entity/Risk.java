package com.example.assetservice.model.entity;

import com.example.assetservice.model.Asset;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_history")
@Data
@NoArgsConstructor
public class Risk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    /*интегральный риск актива на момент расчёта*/
    @Column(name = "calculated_risk", precision = 19, scale = 2, nullable = false)
    private BigDecimal calculatedRisk;

    /*дата и время расчёта*/
    @CreationTimestamp
    @Column(name = "calculation_date", updatable = false)
    private LocalDateTime calculationDate;

    /*детали расчёта (например, список угроз с ущербом)*/
    @Column(name = "calculation_details", columnDefinition = "TEXT")
    private String calculationDetails;
}