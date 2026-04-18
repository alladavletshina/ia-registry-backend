package com.example.notificationservice.integration;

import com.example.notificationservice.client.UserServiceClient;
import com.example.notificationservice.model.NotificationCreateDto;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.model.UserDto;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class NotificationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        // Отключаем RabbitMQ и Feign для интеграционных тестов
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration");
        registry.add("services.user-service.url", () -> "http://dummy");
    }

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private UserServiceClient userServiceClient;

    @Autowired
    private NotificationService notificationService;

    @Test
    void createAndFindNotification_shouldPersistInDatabase() {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto();
        userDto.setId(userId);
        when(userServiceClient.getUserByKeycloakId(anyString())).thenReturn(userDto);

        NotificationCreateDto createDto = new NotificationCreateDto();
        createDto.setKeyclockId(userId);
        createDto.setType(NotificationType.INFO);
        createDto.setTitle("Integration Test");
        createDto.setMessage("Hello from test");

        NotificationDto created = notificationService.createNotification(createDto);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Integration Test");

        NotificationDto found = notificationService.getNotificationById(created.getId());
        assertThat(found.getMessage()).isEqualTo("Hello from test");
    }
}