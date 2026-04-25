package com.example.assetservice.controller;

import com.example.assetservice.dto.ThreatDto;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.ThreatRepository;
import com.example.assetservice.service.fstec.FstecSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ThreatController.class)
@WithMockUser(roles = "admin")
class ThreatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThreatRepository threatRepository;

    @MockBean
    private FstecSyncService fstecSyncService;

    @Test
    void getAllThreats_shouldReturnPage() throws Exception {
        Threat threat = new Threat();
        threat.setId(1L);
        threat.setName("Test Threat");
        threat.setConfidentiality(true);
        threat.setInclusionDate(LocalDate.now());

        Page<Threat> page = new PageImpl<>(List.of(threat));

        when(threatRepository.findByNameContainingIgnoreCase(eq("Test"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/assets/threats")
                        .param("search", "Test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Threat"));
    }

    @Test
    void getThreatById_shouldReturnThreat() throws Exception {
        Threat threat = new Threat();
        threat.setId(5L);
        threat.setName("Specific Threat");
        when(threatRepository.findById(5L)).thenReturn(java.util.Optional.of(threat));

        mockMvc.perform(get("/api/assets/threats/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Specific Threat"));
    }

    @Test
    void syncThreats_shouldReturn200WhenSuccess() throws Exception {
        when(fstecSyncService.syncThreats()).thenReturn(true);

        mockMvc.perform(post("/api/assets/threats/sync")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Синхронизация выполнена."));
    }

    @Test
    void syncThreats_shouldReturn503WhenSyncFails() throws Exception {
        when(fstecSyncService.syncThreats()).thenReturn(false);

        mockMvc.perform(post("/api/assets/threats/sync")
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("Сервис временно недоступен")));
    }

    @Test
    void syncThreats_shouldReturn503WhenExceptionThrown() throws Exception {
        when(fstecSyncService.syncThreats()).thenThrow(new RuntimeException("Network error"));

        mockMvc.perform(post("/api/assets/threats/sync")
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable()) // 503
                .andExpect(content().string(containsString("Сервис временно недоступен")));
    }
}