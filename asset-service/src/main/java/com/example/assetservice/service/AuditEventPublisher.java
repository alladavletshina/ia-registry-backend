package com.example.assetservice.service;

import com.example.assetservice.config.RabbitMQConfig;
import com.example.assetservice.dto.AuditEventDto;
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
            rabbitTemplate.convertAndSend(RabbitMQConfig.AUDIT_EXCHANGE, "audit.asset", event);
            log.info("Published audit event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }
}
