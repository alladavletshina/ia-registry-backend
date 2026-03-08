package com.example.auditservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;      // денормализованное имя для поиска

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip", length = 45)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity;

    @Column(name = "service_name", length = 50)
    private String serviceName;   // имя сервиса-источника

    @Column(name = "object_id")
    private String objectId;       // идентификатор связанного объекта (например, UUID актива)

    @Column(name = "object_type", length = 50)
    private String objectType;     // тип объекта (Asset, User, Task...)

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
