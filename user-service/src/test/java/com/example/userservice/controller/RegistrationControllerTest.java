package com.example.userservice.controller;

import com.example.userservice.TestSecurityConfig;
import com.example.userservice.dto.request.RegisterRequestDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)   // ← добавляем
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void register_shouldReturnCreated() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("new@example.com");
        request.setPassword("Password123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("123456789");
        request.setPosition("Developer");
        request.setDepartment("IT");

        UserResponseDto response = UserResponseDto.builder().email("new@example.com").build();
        when(userService.register(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }
}