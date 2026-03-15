package com.example.assetservice.model.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "threats")
@Data
@NoArgsConstructor
public class Threat {

    /* Идентификатор УБИ (например, "УБИ.004")*/
    @Id
    private String id;

    /*Наименование УБИ*/
    @Column(nullable = false, length = 500)
    private String name;

    /*Описание*/
    @Column(columnDefinition = "TEXT")
    private String description;

    /*Источник угрозы (характеристика и потенциал нарушителя)*/
    @Column(columnDefinition = "TEXT")
    private String source;

    /*Объект воздействия*/
    private String objectAffected;

    /*Нарушение конфиденциальности (1 - да, 0 - нет)*/
    private boolean confidentiality;

    /*Нарушение целостности*/
    private boolean integrity;

    /*Нарушение доступности*/
    private boolean availability;

    /*Дата включения в БнД УБИ*/
    private LocalDate inclusionDate;

    /*Дата последнего изменения данных*/
    private LocalDate lastModified;

    /*Статус угрозы (например, "Опубликована")*/
    @Column(length = 50)
    private String status;

    /*Замечания*/
    @Column(columnDefinition = "TEXT")
    private String notes;

    /* Дата - когда последний раз синхронизировали с БДУ*/
    private LocalDate syncedAt;
}
