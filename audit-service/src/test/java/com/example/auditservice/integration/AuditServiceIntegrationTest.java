package com.example.auditservice.integration;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditStatsDto;
import com.example.auditservice.model.entity.Severity;
import com.example.auditservice.service.AuditService;
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
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AuditServiceIntegrationTest {

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
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AuditService auditService;

    @Test
    void createAndFindAuditLog_shouldPersistInDatabase() {
        AuditEventDto event = new AuditEventDto();
        event.setAction("USER_LOGIN");
        event.setDetails("User admin logged in");
        event.setSeverity(Severity.SUCCESS);
        event.setUserId(UUID.randomUUID());
        event.setUsername("admin");
        event.setIp("192.168.1.10");

        var saved = auditService.createAuditLog(event);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAction()).isEqualTo("USER_LOGIN");
    }

    @Test
    void getStats_shouldReflectSavedData() {
        LocalDate today = LocalDate.now();

        AuditEventDto e1 = new AuditEventDto();
        e1.setAction("A1"); e1.setSeverity(Severity.INFO); e1.setUserId(UUID.randomUUID()); e1.setUsername("u1");
        AuditEventDto e2 = new AuditEventDto();
        e2.setAction("A2"); e2.setSeverity(Severity.WARNING); e2.setUserId(UUID.randomUUID()); e2.setUsername("u1");

        auditService.createAuditLog(e1);
        auditService.createAuditLog(e2);

        AuditStatsDto stats = auditService.getStats(today.minusDays(1), today.plusDays(1));
        assertThat(stats.getTotal()).isGreaterThanOrEqualTo(2);
        assertThat(stats.getInfo()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getWarning()).isGreaterThanOrEqualTo(1);
    }
}