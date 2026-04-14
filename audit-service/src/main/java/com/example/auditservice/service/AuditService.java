package com.example.auditservice.service;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditLogDto;
import com.example.auditservice.model.dto.AuditStatsDto;
import com.example.auditservice.model.entity.AuditLog;
import com.example.auditservice.model.entity.Severity;
import com.example.auditservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Specification<AuditLog> spec = Specification
                .where(hasTimestampAfter(start))
                .and(hasTimestampBefore(end))
                .and(hasUserId(userId))
                .and(hasAction(action))
                .and(hasSeverity(sev))
                .and(globalSearch(search));

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

        AuditLog saved = repository.save(log);
        return AuditLogDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AuditStatsDto getStats(LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.of(2100, 1, 1, 0, 0);

        long total = repository.countByDateRange(start, end);
        long info = repository.countBySeverityAndDateRange(Severity.INFO, start, end);
        long warning = repository.countBySeverityAndDateRange(Severity.WARNING, start, end);
        long danger = repository.countBySeverityAndDateRange(Severity.DANGER, start, end);
        long success = repository.countBySeverityAndDateRange(Severity.SUCCESS, start, end);

        return new AuditStatsDto(total, info, warning, danger, success);
    }

    @Transactional
    public void saveAuditLog(AuditEventDto event) {
        AuditLog log = AuditLog.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .action(event.getAction())
                .details(event.getDetails())
                .ip(event.getIp())
                .severity(event.getSeverity())
                .serviceName(event.getServiceName())
                .objectId(event.getObjectId())
                .objectType(event.getObjectType())
                .build();
        repository.save(log);
    }

    public AuditLogDto getAuditLogById(UUID id) {
        AuditLog auditLog = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No log with such Id"));

        return AuditLogDto.fromEntity(auditLog);
    }

    public List<AuditEventDto> getEventsForPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.of(2100, 1, 1, 0, 0);

        List<AuditLog> logs = repository.findByTimestampBetween(start, end);
        return logs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private AuditEventDto convertToDto(AuditLog log) {

        AuditEventDto dto = new AuditEventDto();
        dto.setUserId(log.getUserId());
        dto.setUsername(log.getUsername());
        dto.setAction(log.getAction());
        dto.setDetails(log.getDetails());
        dto.setIp(log.getIp());
        dto.setSeverity(log.getSeverity());
        dto.setServiceName(log.getServiceName());
        dto.setObjectId(log.getObjectId());
        dto.setObjectType(log.getObjectType());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }

    @Transactional
    public void deleteAuditLog(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись аудита не найдена");
        }
        repository.deleteById(id);
        log.info("Удалена запись аудита id={}", id);
    }

    @Transactional
    public void deleteAllAuditLogs() {
        long count = repository.count();
        repository.deleteAll();
        log.warn("Удалено {} записей аудита", count);
    }
}