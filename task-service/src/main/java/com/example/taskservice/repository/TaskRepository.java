package com.example.taskservice.repository;

import com.example.taskservice.model.Task;
import com.example.taskservice.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;


public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /*Подсчёт задач по исполнителю*/
    long countByAssignedTo(UUID targetUserId);

    /*Подсчёт задач по статусу и исполнителю (если assignedTo == null — считаем все)*/
    @Query("SELECT COUNT(t) FROM Task t WHERE " +
            "(:assignedTo IS NULL OR t.assignedTo = :assignedTo) AND t.status = :status")
    long countByStatusAndAssignedTo(@Param("status") TaskStatus status,
                                    @Param("assignedTo") UUID assignedTo);

    /*Подсчёт просроченных задач (dueDate < today и статус не COMPLETED)*/
    @Query("SELECT COUNT(t) FROM Task t WHERE " +
            "(:assignedTo IS NULL OR t.assignedTo = :assignedTo) " +
            "AND t.status <> 'COMPLETED' AND t.dueDate < :today")
    long countOverdue(@Param("assignedTo") UUID assignedTo, @Param("today") LocalDate today);
}
