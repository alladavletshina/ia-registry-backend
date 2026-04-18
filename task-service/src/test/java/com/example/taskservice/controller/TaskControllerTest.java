package com.example.taskservice.controller;

import com.example.taskservice.model.TaskPriority;
import com.example.taskservice.model.TaskStatus;
import com.example.taskservice.model.TaskType;
import com.example.taskservice.model.request.TaskCreateDto;
import com.example.taskservice.model.request.TaskUpdateDto;
import com.example.taskservice.model.response.TaskDto;
import com.example.taskservice.model.statistics.TaskStatsDto;
import com.example.taskservice.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "admin")
    void createTask_shouldReturn201() throws Exception {
        TaskCreateDto createDto = new TaskCreateDto();
        createDto.setTitle("New Task");
        createDto.setPriority(TaskPriority.MEDIUM);
        createDto.setType(TaskType.REPORT);

        TaskDto responseDto = new TaskDto();
        responseDto.setId(1L);
        responseDto.setTitle("New Task");

        when(taskService.createTask(any(TaskCreateDto.class), any(), anyString())).thenReturn(responseDto);

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Task"));
    }

    @Test
    @WithMockUser(roles = "user")
    void getTaskById_shouldReturn200() throws Exception {
        TaskDto dto = new TaskDto();
        dto.setId(1L);
        dto.setTitle("My Task");
        when(taskService.getTaskById(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "admin")
    void deleteTask_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "user")
    void updateTask_shouldReturn200() throws Exception {
        TaskUpdateDto updateDto = new TaskUpdateDto();
        updateDto.setStatus(TaskStatus.COMPLETED);

        TaskDto response = new TaskDto();
        response.setId(1L);
        response.setStatus(TaskStatus.COMPLETED);

        when(taskService.updateTask(eq(1L), any(TaskUpdateDto.class), any(), anyString())).thenReturn(response);

        mockMvc.perform(patch("/api/tasks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "admin")
    void getStats_shouldReturnStats() throws Exception {
        TaskStatsDto stats = new TaskStatsDto(10, 2, 3, 4, 1);
        when(taskService.getStats(any())).thenReturn(stats);

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.overdue").value(1));
    }

    @Test
    @WithMockUser(roles = "user")
    void getTasks_withFilters_shouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .param("status", "PENDING")
                        .param("priority", "HIGH")
                        .param("search", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    void updateTaskFields_shouldReturn200() throws Exception {
        Map<String, Object> updates = Map.of("status", "OVERDUE");
        TaskDto updated = new TaskDto();
        updated.setId(1L);
        updated.setStatus(TaskStatus.OVERDUE);

        when(taskService.updateTaskFields(eq(1L), anyMap(), any(), anyString())).thenReturn(updated);

        mockMvc.perform(patch("/api/tasks/1/update")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OVERDUE"));
    }
}