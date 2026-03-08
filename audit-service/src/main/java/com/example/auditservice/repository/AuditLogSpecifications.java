package com.example.auditservice.repository;

import com.example.auditservice.model.entity.AuditLog;
import com.example.auditservice.model.entity.Severity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLogSpecifications {

    public static Specification<AuditLog> hasTimestampAfter(LocalDateTime startDate) {
        return (root, query, cb) -> startDate == null ? null : cb.greaterThanOrEqualTo(root.get("timestamp"), startDate);
    }

    public static Specification<AuditLog> hasTimestampBefore(LocalDateTime endDate) {
        return (root, query, cb) -> endDate == null ? null : cb.lessThanOrEqualTo(root.get("timestamp"), endDate);
    }

    public static Specification<AuditLog> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> !StringUtils.hasText(action) ? null : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasSeverity(Severity severity) {
        return (root, query, cb) -> severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    // Если нужен поиск по username (текстовый)
    public static Specification<AuditLog> usernameContains(String search) {
        return (root, query, cb) -> !StringUtils.hasText(search) ? null :
                cb.like(cb.lower(root.get("username")), "%" + search.toLowerCase() + "%");
    }
}
