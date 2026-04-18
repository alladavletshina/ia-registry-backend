package com.example.auditservice.controller;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditLogDto;
import com.example.auditservice.model.dto.AuditStatsDto;
import com.example.auditservice.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@WithMockUser(roles = "admin")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditService auditService;

    @Test
    void createAuditLog_shouldReturn200() throws Exception {
        AuditEventDto request = new AuditEventDto();
        request.setAction("TEST_ACTION");
        AuditLogDto response = new AuditLogDto();
        response.setAction("TEST_ACTION");
        response.setSeverity("info");
        when(auditService.createAuditLog(any())).thenReturn(response);

        mockMvc.perform(post("/api/audit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("TEST_ACTION"));
    }

    @Test
    void getAuditLogById_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        AuditLogDto dto = new AuditLogDto();
        dto.setId(id);
        dto.setAction("VIEW");
        when(auditService.getAuditLogById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/audit/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("VIEW"));
    }

    @Test
    void getStats_shouldReturnStats() throws Exception {
        AuditStatsDto stats = new AuditStatsDto(10, 5, 3, 1, 1);
        when(auditService.getStats(any(), any())).thenReturn(stats);

        mockMvc.perform(get("/api/audit/stats")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    void deleteAuditLog_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(auditService).deleteAuditLog(id);

        mockMvc.perform(delete("/api/audit/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());
        verify(auditService).deleteAuditLog(id);
    }

    @Test
    void getAuditLogs_withFilters_shouldReturnPage() throws Exception {
        AuditLogDto dto = new AuditLogDto();
        dto.setAction("LOGIN");
        Page<AuditLogDto> page = new PageImpl<>(Collections.singletonList(dto));
        when(auditService.getAuditLogs(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/audit")
                        .param("search", "login")
                        .param("severity", "INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"));
    }
}