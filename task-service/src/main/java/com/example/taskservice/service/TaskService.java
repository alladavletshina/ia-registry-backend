package com.example.taskservice.service;

import com.example.taskservice.model.Task;
import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import com.example.taskservice.model.request.AuditEventDto;
import com.example.taskservice.model.request.TaskCreateDto;
import com.example.taskservice.model.request.TaskUpdateDto;
import com.example.taskservice.model.response.NotificationCreateDto;
import com.example.taskservice.model.response.TaskDto;
import com.example.taskservice.model.statistics.TaskStatsDto;
import com.example.taskservice.repository.TaskRepository;
import com.example.taskservice.repository.TaskSpecifications;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final NotificationEventPublisher notificationEventPublisher;

    public TaskDto getTaskById(long id, Jwt jwt) {
        Task task = findTaskOrThrow(id);
        return mapToDto(task);
    }

    private void checkAccess(Task task, Jwt jwt) {
        UUID currentUserId = extractUserId(jwt);
        if (!hasAdminRole(jwt) && !task.getAssignedTo().equals(currentUserId)) {
            throw new AccessDeniedException("You don't have permission to access this task");
        }
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean hasAdminRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return false;

        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List<?>) {
            List<?> roles = (List<?>) rolesObj;
            return roles.stream().anyMatch("admin"::equals);
        }
        return false;
    }

    private TaskDto mapToDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setType(task.getType());
        dto.setDueDate(task.getDueDate());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setEstimatedTime(task.getEstimatedTime());
        dto.setTags(task.getTags());
        dto.setAssetId(task.getAssetId());
        dto.setAssetName(task.getAssetName());
        dto.setAssignedTo(task.getAssignedTo());
        dto.setAssignedBy(task.getAssignedBy());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }

    private Task findTaskOrThrow(long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public TaskDto createTask(@Valid TaskCreateDto dto, Jwt jwt, String clientIp) {
        UUID currentUserId = extractUserId(jwt);

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setPriority(dto.getPriority());
        task.setType(dto.getType());
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(dto.getDueDate());
        task.setEstimatedTime(dto.getEstimatedTime());
        task.setTags(dto.getTags() != null ? dto.getTags() : List.of());
        task.setAssetId(dto.getAssetId());
        task.setAssignedTo(dto.getAssignedTo() != null ? dto.getAssignedTo() : currentUserId);
        task.setAssignedBy(currentUserId);
        task.setCreatedAt(LocalDate.now());
        task.setUpdatedAt(LocalDate.now());

        Task saved = taskRepository.save(task);

        // Уведомление исполнителю (если он не создатель)
        if (saved.getAssignedTo() != null && !saved.getAssignedTo().equals(currentUserId)) {
            NotificationCreateDto notification = new NotificationCreateDto();
            notification.setKeyclockId(saved.getAssignedTo());
            notification.setType("ASSIGNMENT");
            notification.setTitle("Вам назначена задача");
            notification.setMessage(String.format("Задача: %s", saved.getTitle()));
            notification.setActionUrl("/user/tasks/" + saved.getId());
            notification.setActionLabel("Перейти");
            notificationEventPublisher.publishNotification(notification);
        }

        // Уведомление создателю (администратору)
        NotificationCreateDto notifCreator = new NotificationCreateDto();
        notifCreator.setKeyclockId(currentUserId);
        notifCreator.setType("INFO");
        notifCreator.setTitle("Задача создана");
        String assignedToName = saved.getAssignedTo() != null ? saved.getAssignedTo().toString() : "не назначен";
        notifCreator.setMessage(String.format("Вы создали задачу \"%s\" для пользователя %s", saved.getTitle(), assignedToName));
        notifCreator.setActionUrl("/admin/tasks/" + saved.getId());
        notifCreator.setActionLabel("Открыть");
        notificationEventPublisher.publishNotification(notifCreator);

        // Аудит
        AuditEventDto event = new AuditEventDto();
        event.setUserId(currentUserId);
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("TASK_CREATE");
        event.setDetails(String.format("Создана задача: %s (id=%d)", saved.getTitle(), saved.getId()));
        event.setSeverity("INFO");
        event.setServiceName("task-service");
        event.setObjectId(String.valueOf(saved.getId()));
        event.setObjectType("Task");
        event.setIp(clientIp);
        auditEventPublisher.publishEvent(event);

        return mapToDto(saved);
    }

    public void deleteTask(long id, Jwt jwt, String clientIp) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found with id: " + id);
        }

        UUID currentUserId = extractUserId(jwt);
        Task task = findTaskOrThrow(id);

        String taskTitle = task.getTitle();
        UUID assignedTo = task.getAssignedTo();
        UUID assignedBy = task.getAssignedBy();

        // Уведомление исполнителю (если он существует и не является удаляющим)
        if (assignedTo != null && !assignedTo.equals(currentUserId)) {
            NotificationCreateDto notif = new NotificationCreateDto();
            notif.setKeyclockId(assignedTo);
            notif.setType("WARNING");
            notif.setTitle("Задача удалена");
            notif.setMessage(String.format("Задача \"%s\" была удалена", taskTitle));
            notif.setActionUrl("/user/tasks");
            notif.setActionLabel("Список задач");
            notificationEventPublisher.publishNotification(notif);
        }

        // Уведомление создателю (если он существует, не является удаляющим и не совпадает с исполнителем)
        if (assignedBy != null && !assignedBy.equals(currentUserId) && !assignedBy.equals(assignedTo)) {
            NotificationCreateDto notifCreator = new NotificationCreateDto();
            notifCreator.setKeyclockId(assignedBy);
            notifCreator.setType("WARNING");
            notifCreator.setTitle("Задача удалена");
            notifCreator.setMessage(String.format("Задача \"%s\" (исполнитель %s) была удалена",
                    taskTitle, assignedTo != null ? assignedTo.toString() : "не назначен"));
            notifCreator.setActionUrl("/admin/tasks");
            notifCreator.setActionLabel("Список задач");
            notificationEventPublisher.publishNotification(notifCreator);
        }

        // Удаляем задачу
        taskRepository.deleteById(id);

        // Отправляем событие аудита
        AuditEventDto event = new AuditEventDto();
        event.setUserId(currentUserId);
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("TASK_DELETE");
        event.setDetails(String.format("Удалена задача id=%d: %s", id, task.getTitle()));
        event.setSeverity("WARNING");
        event.setServiceName("task-service");
        event.setObjectId(String.valueOf(id));
        event.setObjectType("Task");
        event.setIp(clientIp);
        auditEventPublisher.publishEvent(event);
    }

    public TaskDto updateTask(long id, TaskUpdateDto dto, Jwt jwt, String clientIp) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, jwt);

        boolean isAdmin = hasAdminRole(jwt);
        TaskStatus oldStatus = task.getStatus();

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        if (dto.getType() != null) task.setType(dto.getType());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());
        if (dto.getDueDate() != null) task.setDueDate(dto.getDueDate());
        if (dto.getEstimatedTime() != null) task.setEstimatedTime(dto.getEstimatedTime());
        if (dto.getTags() != null) task.setTags(dto.getTags());
        if (dto.getAssetId() != null) task.setAssetId(dto.getAssetId());
        if (dto.getAssetName() != null) task.setAssetName(dto.getAssetName());

        if (dto.getAssignedTo() != null) {
            if (!isAdmin) {
                throw new AccessDeniedException("Only admin can reassign task");
            }
            task.setAssignedTo(dto.getAssignedTo());
        }

        task.setUpdatedAt(LocalDate.now());

        // Уведомления при изменении статуса
        if (dto.getStatus() != null && !dto.getStatus().equals(oldStatus)) {
            // Исполнителю
            if (task.getAssignedTo() != null) {
                NotificationCreateDto notif = new NotificationCreateDto();
                notif.setKeyclockId(task.getAssignedTo());
                notif.setType("INFO");
                notif.setTitle("Статус задачи изменён");
                notif.setMessage(String.format("Задача \"%s\" теперь имеет статус %s", task.getTitle(), dto.getStatus()));
                notif.setActionUrl("/user/tasks/" + task.getId());
                notif.setActionLabel("Открыть");
                notificationEventPublisher.publishNotification(notif);
            }
            // Создателю (если он не исполнитель)
            if (task.getAssignedBy() != null && !task.getAssignedBy().equals(task.getAssignedTo())) {
                NotificationCreateDto notifCreator = new NotificationCreateDto();
                notifCreator.setKeyclockId(task.getAssignedBy());
                notifCreator.setType("INFO");
                notifCreator.setTitle("Статус задачи изменён");
                notifCreator.setMessage(String.format("Задача \"%s\" (исполнитель %s) изменила статус на %s",
                        task.getTitle(), task.getAssignedTo() != null ? task.getAssignedTo().toString() : "не назначен", dto.getStatus()));
                notifCreator.setActionUrl("/admin/tasks/" + task.getId());
                notifCreator.setActionLabel("Открыть");
                notificationEventPublisher.publishNotification(notifCreator);
            }
        }

        AuditEventDto event = new AuditEventDto();
        event.setUserId(extractUserId(jwt));
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("TASK_UPDATE");
        event.setDetails(String.format("Обновлена задача id=%d: %s", id, task.getTitle()));
        event.setSeverity("INFO");
        event.setServiceName("task-service");
        event.setObjectId(String.valueOf(id));
        event.setObjectType("Task");
        event.setIp(clientIp);
        auditEventPublisher.publishEvent(event);

        return mapToDto(task);
    }

    public Page<TaskDto> findTasks(TaskStatus status, TaskPriority priority, TaskType type,
                                   String search, Long assetId, UUID assignedTo, LocalDate dueDateFrom,
                                   LocalDate dueDateTo, Pageable pageable, Jwt jwt, UUID userId) {

        boolean isAdmin = hasAdminRole(jwt);
        if (!isAdmin) {
            assignedTo = userId;
        }

        Specification<Task> spec = Specification
                .where(TaskSpecifications.byStatus(status))
                .and(TaskSpecifications.byPriority(priority))
                .and(TaskSpecifications.byType(type))
                .and(TaskSpecifications.byAssetId(assetId))
                .and(TaskSpecifications.byAssignedTo(assignedTo))
                .and(TaskSpecifications.byDueDateBetween(dueDateFrom, dueDateTo))
                .and(TaskSpecifications.search(search));

        return taskRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    public TaskStatsDto getStats(Jwt jwt) {
        UUID currentUserId = extractUserId(jwt);
        boolean isAdmin = hasAdminRole(jwt);

        long total, pending, inProgress, completed, overdue;

        if (isAdmin) {
            total = taskRepository.count();
            pending = taskRepository.countByStatus(TaskStatus.PENDING);
            inProgress = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
            completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
            overdue = taskRepository.countOverdueAll(LocalDate.now());
        } else {
            total = taskRepository.countByAssignedTo(currentUserId);
            pending = taskRepository.countByStatusAndAssignedTo(TaskStatus.PENDING, currentUserId);
            inProgress = taskRepository.countByStatusAndAssignedTo(TaskStatus.IN_PROGRESS, currentUserId);
            completed = taskRepository.countByStatusAndAssignedTo(TaskStatus.COMPLETED, currentUserId);
            overdue = taskRepository.countOverdueForUser(currentUserId, LocalDate.now());
        }

        return new TaskStatsDto(total, pending, inProgress, completed, overdue);
    }

    public TaskDto updateTaskFields(long id, Map<String, Object> updates, Jwt jwt, String clientIp) {
        Task task = findTaskOrThrow(id);
        boolean changed = false;
        TaskStatus oldStatus = task.getStatus();

        if (updates.containsKey("status")) {
            String newStatusStr = (String) updates.get("status");
            try {
                TaskStatus newStatus = TaskStatus.valueOf(newStatusStr);
                task.setStatus(newStatus);
                changed = true;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status value: " + newStatusStr);
            }
        }

        if (updates.containsKey("dueDate")) {
            String dueDateStr = (String) updates.get("dueDate");
            try {
                LocalDate newDueDate = LocalDate.parse(dueDateStr);
                task.setDueDate(newDueDate);
                changed = true;
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Invalid dueDate format. Use YYYY-MM-DD");
            }
        }

        if (!changed) {
            throw new RuntimeException("No valid fields to update");
        }

        task.setUpdatedAt(LocalDate.now());

        // Уведомления при изменении статуса (если он изменился)
        if (updates.containsKey("status") && !task.getStatus().equals(oldStatus)) {
            // Исполнителю
            if (task.getAssignedTo() != null) {
                NotificationCreateDto notif = new NotificationCreateDto();
                notif.setKeyclockId(task.getAssignedTo());
                notif.setType("INFO");
                notif.setTitle("Статус задачи изменён");
                notif.setMessage(String.format("Задача \"%s\" теперь имеет статус %s", task.getTitle(), task.getStatus()));
                notif.setActionUrl("/user/tasks/" + task.getId());
                notif.setActionLabel("Открыть");
                notificationEventPublisher.publishNotification(notif);
            }
            // Создателю
            if (task.getAssignedBy() != null && !task.getAssignedBy().equals(task.getAssignedTo())) {
                NotificationCreateDto notifCreator = new NotificationCreateDto();
                notifCreator.setKeyclockId(task.getAssignedBy());
                notifCreator.setType("INFO");
                notifCreator.setTitle("Статус задачи изменён");
                notifCreator.setMessage(String.format("Задача \"%s\" (исполнитель %s) изменила статус на %s",
                        task.getTitle(), task.getAssignedTo() != null ? task.getAssignedTo().toString() : "не назначен", task.getStatus()));
                notifCreator.setActionUrl("/admin/tasks/" + task.getId());
                notifCreator.setActionLabel("Открыть");
                notificationEventPublisher.publishNotification(notifCreator);
            }
        }

        UUID currentUserId = extractUserId(jwt);
        AuditEventDto event = new AuditEventDto();
        event.setUserId(currentUserId);
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("TASK_UPDATE_FIELDS");
        event.setDetails(String.format("Частичное обновление задачи id=%d", id));
        event.setSeverity("INFO");
        event.setServiceName("task-service");
        event.setObjectId(String.valueOf(id));
        event.setObjectType("Task");
        event.setIp(clientIp);
        auditEventPublisher.publishEvent(event);

        return mapToDto(task);
    }
}