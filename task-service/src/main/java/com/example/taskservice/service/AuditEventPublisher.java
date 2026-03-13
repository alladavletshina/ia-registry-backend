package com.example.taskservice.service;

import com.example.taskservice.config.RabbitMQConfig;
import com.example.taskservice.model.request.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEvent(AuditEventDto event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.AUDIT_EXCHANGE, "audit.task", event);
            log.debug("Audit event published: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }
}