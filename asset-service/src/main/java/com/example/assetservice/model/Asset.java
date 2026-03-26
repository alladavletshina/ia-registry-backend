package com.example.assetservice.model;

import com.example.assetservice.model.entity.AssetGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Data
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    private String name;

    private String category;

    @Column(name = "owner_id")
    private String ownerId;

    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    @Enumerated(EnumType.STRING)
    private CIA confidentiality;

    @Enumerated(EnumType.STRING)
    private CIA integrity;

    @Enumerated(EnumType.STRING)
    private CIA availability;

    /*стоимость актива*/
    @Column(precision = 19, scale = 2)
    private BigDecimal value;

    /* вес конфиденциальности*/
    @Column(name = "weight_c", nullable = false)
    private Integer weightC = 1;

    /*вес целостности*/
    @Column(name = "weight_i", nullable = false)
    private Integer weightI = 1;

    /*вес доступности*/
    @Column(name = "weight_a", nullable = false)
    private Integer weightA = 1;

    /*правовой статус*/
    @Column(name = "legal_status")
    private String legalStatus;

    /* группа активов */
    @ManyToOne
    @JoinColumn(name = "group_id")
    private AssetGroup group;

    private String lastReview;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    private String tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
