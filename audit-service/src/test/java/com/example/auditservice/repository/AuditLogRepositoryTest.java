package com.example.auditservice.repository;

import com.example.auditservice.model.entity.AuditLog;
import com.example.auditservice.model.entity.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=PostgreSQL"
})
class AuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository repository;

    @Test
    void shouldSaveAndFindById() {
        AuditLog log = createAuditLog(Severity.INFO, "LOGIN");
        AuditLog saved = entityManager.persistAndFlush(log);
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void findByTimestampBetween_shouldReturnLogsInRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        AuditLog log = createAuditLog(Severity.DANGER, "SECURITY_CHECK");
        // @PrePersist установит timestamp = сейчас, что точно в диапазоне
        entityManager.persistAndFlush(log);

        List<AuditLog> result = repository.findByTimestampBetween(start, end);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("SECURITY_CHECK");
    }

    private AuditLog createAuditLog(Severity severity, String action) {
        return AuditLog.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .action(action)
                .details("Test details")
                .ip("127.0.0.1")
                .severity(severity)
                .serviceName("test-service")
                .objectId("obj-1")
                .objectType("User")
                .timestamp(LocalDateTime.now()) // будет перезаписано @PrePersist – это нормально
                .build();
    }
}