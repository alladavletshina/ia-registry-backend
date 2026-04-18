package com.example.taskservice.integration;

import com.example.taskservice.model.Task;
import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import com.example.taskservice.model.request.TaskCreateDto;
import com.example.taskservice.model.request.TaskUpdateDto;
import com.example.taskservice.model.response.TaskDto;
import com.example.taskservice.repository.TaskRepository;
import com.example.taskservice.service.AuditEventPublisher;
import com.example.taskservice.service.NotificationEventPublisher;
import com.example.taskservice.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
@Import(TaskServiceIntegrationTest.TestConfig.class)   // явно импортируем конфигурацию
class TaskServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    // Тестовая конфигурация для подмены ConnectionFactory моком
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        // Отключаем автоконфигурацию RabbitMQ (один раз, в одном месте)
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration");
    }

    @MockBean
    private AuditEventPublisher auditEventPublisher;

    @MockBean
    private NotificationEventPublisher notificationEventPublisher;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    private Jwt mockJwt;
    private UUID currentUserId;
    private UUID assignedUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        assignedUserId = UUID.randomUUID();
        mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject()).thenReturn(currentUserId.toString());
        when(mockJwt.getClaim("preferred_username")).thenReturn("integration-user");
        // Стаб для realm_access (по умолчанию пользователь не админ)
        when(mockJwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of()));
    }

    @Test
    void createAndFindTask_shouldPersistInDatabase() {
        TaskCreateDto createDto = new TaskCreateDto();
        createDto.setTitle("Integration Task");
        createDto.setPriority(TaskPriority.HIGH);
        createDto.setType(TaskType.REPORT);
        createDto.setDueDate(LocalDate.now().plusDays(7));
        createDto.setAssignedTo(assignedUserId);

        TaskDto created = taskService.createTask(createDto, mockJwt, "127.0.0.1");

        assertThat(created.getId()).isPositive();
        assertThat(created.getTitle()).isEqualTo("Integration Task");
        assertThat(created.getAssignedTo()).isEqualTo(assignedUserId);

        var saved = taskRepository.findById(created.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void updateTask_shouldModifyAndPersist() {
        TaskCreateDto createDto = new TaskCreateDto();
        createDto.setTitle("Original");
        createDto.setPriority(TaskPriority.LOW);
        createDto.setType(TaskType.INVENTORY);
        TaskDto created = taskService.createTask(createDto, mockJwt, "127.0.0.1");

        TaskUpdateDto updateDto = new TaskUpdateDto();
        updateDto.setTitle("Updated");
        updateDto.setStatus(TaskStatus.IN_PROGRESS);
        TaskDto updated = taskService.updateTask(created.getId(), updateDto, mockJwt, "127.0.0.1");

        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        Task fromDb = taskRepository.findById(created.getId()).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("Updated");
        assertThat(fromDb.getUpdatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void deleteTask_shouldRemoveFromDatabase() {
        TaskCreateDto createDto = new TaskCreateDto();
        createDto.setTitle("To Delete");
        createDto.setPriority(TaskPriority.MEDIUM);
        createDto.setType(TaskType.BACKUP);
        TaskDto created = taskService.createTask(createDto, mockJwt, "127.0.0.1");

        taskService.deleteTask(created.getId(), mockJwt, "127.0.0.1");

        assertThat(taskRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void getStats_shouldReflectActualData() {
        // Делаем пользователя администратором для этого теста
        when(mockJwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("admin")));

        TaskCreateDto dto1 = new TaskCreateDto();
        dto1.setTitle("Task1");
        dto1.setPriority(TaskPriority.HIGH);
        dto1.setType(TaskType.REVIEW);
        taskService.createTask(dto1, mockJwt, "127.0.0.1");

        TaskCreateDto dto2 = new TaskCreateDto();
        dto2.setTitle("Task2");
        dto2.setPriority(TaskPriority.MEDIUM);
        dto2.setType(TaskType.UPDATE);
        dto2.setAssignedTo(assignedUserId);
        taskService.createTask(dto2, mockJwt, "127.0.0.1");

        var stats = taskService.getStats(mockJwt);

        assertThat(stats.getTotal()).isEqualTo(2);
        assertThat(stats.getPending()).isEqualTo(2);
        assertThat(stats.getCompleted()).isEqualTo(0);
    }
}