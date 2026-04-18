package com.example.taskservice.repository;

import com.example.taskservice.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void shouldSaveAndFindTask() {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.PENDING);
        task.setType(TaskType.UPDATE);
        task.setAssignedBy(UUID.randomUUID());
        task.setCreatedAt(LocalDate.now());
        task.setUpdatedAt(LocalDate.now());

        Task saved = entityManager.persistAndFlush(task);

        Optional<Task> found = taskRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Task");
    }

    @Test
    void countByStatusAndAssignedTo_shouldReturnCorrectCount() {
        UUID userId = UUID.randomUUID();
        Task task1 = createTask(userId, TaskStatus.PENDING);
        Task task2 = createTask(userId, TaskStatus.PENDING);
        Task task3 = createTask(userId, TaskStatus.COMPLETED);
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        long count = taskRepository.countByStatusAndAssignedTo(TaskStatus.PENDING, userId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void countOverdueForUser_shouldReturnOverdueTasks() {
        UUID userId = UUID.randomUUID();
        Task overdueTask = createTask(userId, TaskStatus.IN_PROGRESS);
        overdueTask.setDueDate(LocalDate.now().minusDays(1));
        Task notOverdue = createTask(userId, TaskStatus.IN_PROGRESS);
        notOverdue.setDueDate(LocalDate.now().plusDays(1));
        entityManager.persist(overdueTask);
        entityManager.persist(notOverdue);
        entityManager.flush();

        long overdueCount = taskRepository.countOverdueForUser(userId, LocalDate.now());
        assertThat(overdueCount).isEqualTo(1);
    }

    @Test
    void findOverdueTasks_shouldReturnList() {
        UUID userId = UUID.randomUUID();
        Task overdue = createTask(userId, TaskStatus.PENDING);
        overdue.setDueDate(LocalDate.now().minusDays(2));
        Task completedOverdue = createTask(userId, TaskStatus.COMPLETED);
        completedOverdue.setDueDate(LocalDate.now().minusDays(1)); // completed не считается просроченной
        entityManager.persist(overdue);
        entityManager.persist(completedOverdue);
        entityManager.flush();

        List<Task> overdueTasks = taskRepository.findOverdueTasks();
        assertThat(overdueTasks).hasSize(1);
        assertThat(overdueTasks.get(0).getId()).isEqualTo(overdue.getId());
    }

    private Task createTask(UUID assignedTo, TaskStatus status) {
        Task task = new Task();
        task.setTitle("Task");
        task.setPriority(TaskPriority.MEDIUM);
        task.setType(TaskType.REVIEW);
        task.setStatus(status);
        task.setAssignedTo(assignedTo);
        task.setAssignedBy(UUID.randomUUID());
        task.setCreatedAt(LocalDate.now());
        task.setUpdatedAt(LocalDate.now());
        return task;
    }
}