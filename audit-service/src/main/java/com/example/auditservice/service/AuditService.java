package com.example.auditservice.service;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditLogDto;
import com.example.auditservice.model.entity.AuditLog;
import com.example.auditservice.model.entity.Severity;
import com.example.auditservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static com.example.auditservice.repository.AuditLogSpecifications.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(
            LocalDate startDate,
            LocalDate endDate,
            UUID userId,
            String action,
            String severity,
            String search,
            Pageable pageable) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Severity sev = null;
        if (severity != null) {
            try {
                sev = Severity.valueOf(severity.toUpperCase());
            } catch (IllegalArgumentException e) {
                // игнорируем
            }
        }

        // Строим спецификацию динамически
        Specification<AuditLog> spec = Specification
                .where(hasTimestampAfter(start))
                .and(hasTimestampBefore(end))
                .and(hasUserId(userId))
                .and(hasAction(action))
                .and(hasSeverity(sev))
                .and(usernameContains(search)); // если нужно искать по username

        Page<AuditLog> page = repository.findAll(spec, pageable);
        return page.map(AuditLogDto::fromEntity);
    }

    @Transactional
    public AuditLogDto createAuditLog(AuditEventDto dto) {
        AuditLog log = AuditLog.builder()
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .action(dto.getAction())
                .details(dto.getDetails())
                .ip(dto.getIp())
                .severity(dto.getSeverity() != null ? dto.getSeverity() : Severity.INFO)
                .serviceName(dto.getServiceName())
                .objectId(dto.getObjectId())
                .objectType(dto.getObjectType())
                .build();
        // timestamp проставится автоматически через @PrePersist
        AuditLog saved = repository.save(log);
        return AuditLogDto.fromEntity(saved);
    }

}