package com.example.taskservice.service;

import com.example.taskservice.config.NotificationRabbitConfig;
import com.example.taskservice.model.response.NotificationCreateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishNotification(NotificationCreateDto dto) {
        try {
            rabbitTemplate.convertAndSend(
                    NotificationRabbitConfig.NOTIFICATION_EXCHANGE,
                    "notification.task",
                    dto
            );
            log.info("Notification published: {}", dto);
        } catch (Exception e) {
            log.error("Failed to publish notification: {}", e.getMessage(), e);
        }
    }
}
