package com.example.notificationservice.repository;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository repository;

    private Notification createNotification(UUID userId, boolean isRead) {
        return Notification.builder()
                .keyclockId(userId)
                .type(NotificationType.INFO)
                .title("Test")
                .message("Message")
                .read(isRead)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldSaveAndFindById() {
        UUID userId = UUID.randomUUID();
        Notification notification = createNotification(userId, false);
        Notification saved = entityManager.persistAndFlush(notification);

        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void findByKeyclockId_shouldReturnPaged() {
        UUID userId = UUID.randomUUID();
        Notification n1 = createNotification(userId, false);
        Notification n2 = createNotification(userId, true);
        entityManager.persist(n1);
        entityManager.persist(n2);
        entityManager.flush();

        Page<Notification> page = repository.findByKeyclockId(userId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByKeyclockIdAndReadFalse_shouldReturnUnreadOnly() {
        UUID userId = UUID.randomUUID();
        Notification unread = createNotification(userId, false);
        Notification read = createNotification(userId, true);
        entityManager.persist(unread);
        entityManager.persist(read);
        entityManager.flush();

        Page<Notification> page = repository.findByKeyclockIdAndReadFalse(userId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).isRead()).isFalse();
    }

    @Test
    void markAllAsRead_shouldUpdateAllUnread() {
        UUID userId = UUID.randomUUID();
        Notification n1 = createNotification(userId, false);
        Notification n2 = createNotification(userId, false);
        entityManager.persist(n1);
        entityManager.persist(n2);
        entityManager.flush();

        repository.markAllAsRead(userId); // возвращает void
        entityManager.flush();
        entityManager.clear();

        // Проверяем, что все уведомления стали прочитанными
        Page<Notification> page = repository.findByKeyclockId(userId, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(Notification::isRead);
    }

    @Test
    void countByKeyclockIdAndReadFalse_shouldReturnCorrectCount() {
        UUID userId = UUID.randomUUID();
        entityManager.persist(createNotification(userId, false));
        entityManager.persist(createNotification(userId, false));
        entityManager.persist(createNotification(userId, true));
        entityManager.flush();

        long count = repository.countByKeyclockIdAndReadFalse(userId);
        assertThat(count).isEqualTo(2);
    }
}