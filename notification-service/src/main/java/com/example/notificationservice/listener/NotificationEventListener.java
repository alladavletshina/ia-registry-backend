package com.example.notificationservice.listener;

import com.example.notificationservice.model.NotificationCreateDto;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${notification.queue:notification.queue}")
    public void handleNotification(NotificationCreateDto dto) {
        log.info("Received notification: {}", dto);
        try {
            notificationService.createNotification(dto);
        } catch (Exception e) {
            log.error("Failed to create notification", e);
        }
    }
}
