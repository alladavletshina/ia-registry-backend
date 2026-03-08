package com.example.auditservice.controller;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditLogDto;
import com.example.auditservice.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public ResponseEntity<AuditLogDto> createAuditLog(@RequestBody AuditEventDto eventDto) {
        try {
            AuditLogDto created = auditService.createAuditLog(eventDto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Ошибка при создании аудит-лога: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<AuditLogDto> page = auditService.getAuditLogs(startDate, endDate, userId, action, severity, search, pageable);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            log.error("Ошибка при получении аудит-логов: {}", e.getMessage(), e);
            throw e;
        }
    }
}
