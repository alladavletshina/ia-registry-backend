package com.example.userservice.repository;

import com.example.userservice.model.UserEntity;
import com.example.userservice.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository repository;

    private UserEntity createUser(String email) {
        UserEntity user = new UserEntity();
        // НЕ устанавливаем id – JPA сгенерирует сам
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void shouldSaveAndFindByEmail() {
        UserEntity user = createUser("test@example.com");
        entityManager.persistAndFlush(user);

        var found = repository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldFindByKeycloakId() {
        UserEntity user = createUser("keycloak@example.com");
        user.setKeycloakId("kc-123");
        entityManager.persistAndFlush(user);

        var found = repository.findByKeycloakId("kc-123");
        assertThat(found).isPresent();
    }

    @Test
    void existsByEmail_shouldReturnTrueWhenExists() {
        UserEntity user = createUser("exists@example.com");
        entityManager.persistAndFlush(user);

        boolean exists = repository.existsByEmail("exists@example.com");
        assertThat(exists).isTrue();
    }
}