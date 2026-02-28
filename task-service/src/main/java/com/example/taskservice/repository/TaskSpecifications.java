package com.example.taskservice.repository;

import com.example.taskservice.model.Task;
import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class TaskSpecifications {
    public static Specification<Task> byStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> byPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> byType(TaskType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Task> byAssetId(Long assetId) {
        return (root, query, cb) -> assetId == null ? cb.conjunction() : cb.equal(root.get("assetId"), assetId);
    }

    public static Specification<Task> byAssignedTo(UUID assignedTo) {
        return (root, query, cb) -> assignedTo == null ? cb.conjunction() : cb.equal(root.get("assignedTo"), assignedTo);
    }

    public static Specification<Task> byDueDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get("dueDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("dueDate"), from);
            return cb.lessThanOrEqualTo(root.get("dueDate"), to);
        };
    }

    public static Specification<Task> search(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) return cb.conjunction();

            String pattern = "%" + searchTerm.toLowerCase() + "%";
            // Поиск по title, description и tags (через join)
            Join<Object, Object> tagsJoin = root.joinList("tags", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(tagsJoin.get("tag")), pattern)
            );
        };
    }
}
