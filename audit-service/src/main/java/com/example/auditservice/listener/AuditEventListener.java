package com.example.auditservice.listener;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditService auditService;

    @RabbitListener(queues = "${audit.queue:audit.queue}")
    public void handleAuditEvent(AuditEventDto event) {
        log.info("Received audit event: {}", event);
        try {
            auditService.saveAuditLog(event);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}