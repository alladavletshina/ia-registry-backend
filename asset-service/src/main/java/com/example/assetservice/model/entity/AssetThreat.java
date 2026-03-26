package com.example.assetservice.model.entity;

import com.example.assetservice.model.Asset;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_threats")
@Data
@NoArgsConstructor
public class AssetThreat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne
    @JoinColumn(name = "threat_id", nullable = false)
    private Threat threat;

    /* вероятность реализации (0..1) */
    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal probability;

    /*переопределённый флаг конфиденциальности (null = использовать из угрозы)*/
    @Column(name = "custom_c")
    private Boolean customC;

    /*переопределённый флаг целостности (null = использовать из угрозы)*/
    @Column(name = "custom_i")
    private Boolean customI;

    /*переопределённый флаг доступности*/
    @Column(name = "custom_a")
    private Boolean customA;

    /*эффективность мер защиты (0..1)*/
    @Column(name = "mitigation_effect", precision = 3, scale = 2)
    private BigDecimal mitigationEffect = BigDecimal.ZERO;

    /*статус связи (ACTIVE, RESOLVED)*/
    @Column(length = 20)
    private String status = "ACTIVE";

    /*дата оценки (привязки/пересмотра)*/
    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (assessmentDate == null) {
            assessmentDate = LocalDate.now();
        }
        if (probability == null) {
            probability = BigDecimal.ZERO;
        }
        if (mitigationEffect == null) {
            mitigationEffect = BigDecimal.ZERO;
        }
    }
}