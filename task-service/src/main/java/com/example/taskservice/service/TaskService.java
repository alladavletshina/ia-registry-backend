package com.example.taskservice.service;

import com.example.taskservice.model.Task;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.request.TaskCreateDto;
import com.example.taskservice.model.response.TaskDto;
import com.example.taskservice.repository.TaskRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskDto getTaskById(long id, Jwt jwt) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, jwt);
        return mapToDto(task);
    }

    private void checkAccess(Task task, Jwt jwt) {
        UUID currentUserId = extractUserId(jwt);
        if(!hasAdminRole(jwt) && !task.getAssignedTo().equals(currentUserId)) {
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

    public TaskDto createTask(@Valid TaskCreateDto dto, Jwt jwt) {
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
        return mapToDto(saved);
    }
}
