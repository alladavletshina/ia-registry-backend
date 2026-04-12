package com.example.taskservice.repository;

import com.example.taskservice.model.Task;
import com.example.taskservice.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // Для администратора: считаем все задачи по статусу
    long countByStatus(TaskStatus status);

    // Для администратора: считаем все просроченные задачи (dueDate < today и статус не COMPLETED)
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status <> 'COMPLETED' AND t.dueDate < :today")
    long countOverdueAll(@Param("today") LocalDate today);

    // Для обычного пользователя: считаем его задачи по статусу
    long countByStatusAndAssignedTo(TaskStatus status, UUID assignedTo);

    // Для обычного пользователя: считаем его просроченные задачи
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo = :assignedTo AND t.status <> 'COMPLETED' AND t.dueDate < :today")
    long countOverdueForUser(@Param("assignedTo") UUID assignedTo, @Param("today") LocalDate today);

    // Для администратора: общее количество задач
    long count();

    // Для обычного пользователя: количество его задач
    long countByAssignedTo(UUID assignedTo);

    // Новый метод: просроченные задачи
    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_DATE AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasks();
}
