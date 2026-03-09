package com.example.auditservice.repository;

import com.example.auditservice.model.entity.AuditLog;
import com.example.auditservice.model.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = :severity AND a.timestamp BETWEEN :start AND :end")
    long countBySeverityAndDateRange(@Param("severity") Severity severity, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT * FROM audit_logs WHERE timestamp BETWEEN :start AND :end", nativeQuery = true)
    List<AuditLog> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
