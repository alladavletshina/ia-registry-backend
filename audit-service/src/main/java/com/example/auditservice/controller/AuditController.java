package com.example.auditservice.controller;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditLogDto;
import com.example.auditservice.model.dto.AuditStatsDto;
import com.example.auditservice.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit Logs", description = "Журнал аудита (только для администраторов)")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;


    @PostMapping
    @Operation(
            summary = "Создать тестовую запись аудита",
            description = "Тестовый метод для добавления записи в журнал аудита. В реальной работе записи создаются автоматически другими сервисами через события."
    )
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<AuditLogDto> createAuditLog(@RequestBody AuditEventDto eventDto) {
        try {
            AuditLogDto created = auditService.createAuditLog(eventDto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Ошибка при создании аудит-лога: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Экспорт в CSV")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String search
    ){
        Page<AuditLogDto> all = auditService.getAuditLogs(startDate, endDate, userId, action, severity, search, Pageable.unpaged());
        StringBuilder csv = new StringBuilder("Время,Пользователь,Действие,Детали,IP адрес,Уровень\n");

        for (AuditLogDto log : all.getContent()) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    log.getTimestamp(),
                    log.getUser(),
                    log.getAction(),
                    log.getDetails().replace("\"", "\"\""),
                    log.getIp(),
                    log.getSeverity()));
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=audit_logs.csv")
                .body(csv.toString());
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика по записям аудита")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<AuditStatsDto> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ){
        AuditStatsDto stats = auditService.getStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получение записи аудита по ИД")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<AuditLogDto> getAuditLogById(
            @PathVariable UUID id
    ){
        AuditLogDto logDto = auditService.getAuditLogById(id);
        return ResponseEntity.ok(logDto);
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('admin')")
    public List<AuditEventDto> getReportData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return auditService.getEventsForPeriod(startDate, endDate);
    }

    @GetMapping
    @Operation(summary = "Получить список записей аудита с пагинацией и фильтрацией")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("getAuditLogs: startDate={}, endDate={}, userId={}, action={}, severity={}, search={}, pageable={}",
                startDate, endDate, userId, action, severity, search, pageable);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }

        UUID userIdUuid = null;
        if (StringUtils.hasText(userId)) {
            try {
                userIdUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId format");
            }
        }

        Page<AuditLogDto> page = auditService.getAuditLogs(
                startDate, endDate, userIdUuid, action, severity, search, pageable
        );
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить запись аудита по ID (ТЕСТОВЫЙ)",
            description = "Удаляет конкретную запись из журнала аудита. Доступно только для администраторов или в тестовом профиле."
    )
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteAuditLog(@PathVariable UUID id) {
        log.warn("Удаление записи аудита id={} (тестовый вызов)", id);
        auditService.deleteAuditLog(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    @Operation(
            summary = "Удалить ВСЕ записи аудита (ТЕСТОВЫЙ, опасно!)",
            description = "Удаляет все строки из таблицы audit_logs. Рекомендуется использовать только в тестовой среде."
    )
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteAllAuditLogs() {
        log.warn("!!! Удаление ВСЕХ записей аудита !!!");
        auditService.deleteAllAuditLogs();
        return ResponseEntity.noContent().build();
    }
}
