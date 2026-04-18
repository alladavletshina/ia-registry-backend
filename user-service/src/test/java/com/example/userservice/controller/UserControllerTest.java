package com.example.userservice.controller;

import com.example.userservice.TestSecurityConfig;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void getAllUsers_shouldReturnList() throws Exception {
        UserResponseDto dto = UserResponseDto.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("Test")
                .build();
        when(userService.getAllUsers()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@example.com"));
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponseDto dto = UserResponseDto.builder().id(id).email("user@example.com").build();
        when(userService.getUserById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "user")
    void getCurrentUser_shouldReturnUser() throws Exception {
        UserResponseDto dto = UserResponseDto.builder().email("current@example.com").build();
        when(userService.getUserByKeycloakId(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))   // ← добавляем
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("current@example.com"));
    }
}