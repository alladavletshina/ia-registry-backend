package com.example.notificationservice.controller;

import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.model.UnreadCountDto;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@WithMockUser(roles = "user")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    void getMyNotifications_shouldReturnPage() throws Exception {
        NotificationDto dto = new NotificationDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Test");
        Page<NotificationDto> page = new PageImpl<>(List.of(dto));
        when(notificationService.getUserNotifications(any(), anyBoolean(), any())).thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test"));
    }

    @Test
    void getNotificationById_shouldReturnDto() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationDto dto = new NotificationDto();
        dto.setId(id);
        dto.setTitle("Hello");
        when(notificationService.getNotificationById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/notifications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Hello"));
    }

    @Test
    void markAsRead_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/notifications/read/{id}", id).with(csrf()))
                .andExpect(status().isOk());
        verify(notificationService).markAsRead(id);
    }

    @Test
    void markAllAsRead_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").with(csrf()))
                .andExpect(status().isOk());
        verify(notificationService).markAllAsRead(any());
    }

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(notificationService.countUnread(any())).thenReturn(7L);
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test
    void deleteNotification_shouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/notifications/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());
        verify(notificationService).deleteNotification(id);
    }

    @Test
    @WithMockUser(roles = "admin")
    void getAllNotifications_shouldReturnPage() throws Exception {
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Admin view");
        Page<NotificationDto> page = new PageImpl<>(List.of(dto));
        when(notificationService.getAllNotifications(eq(true), any())).thenReturn(page);

        mockMvc.perform(get("/api/notifications/admin/all")
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Admin view"));
    }
}