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

import static org.mockito.ArgumentMatchers.any;
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
    void syncThreats_shouldTriggerSyncAndReturn200() throws Exception {
        doNothing().when(fstecSyncService).syncThreats();

        mockMvc.perform(post("/api/assets/threats/sync")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Синхронизация с ФСТЭК успешно запущена. Проверьте логи для деталей."));

        verify(fstecSyncService, times(1)).syncThreats();
    }

    @Test
    void syncThreats_whenException_shouldReturn500() throws Exception {
        doThrow(new RuntimeException("Network error")).when(fstecSyncService).syncThreats();

        mockMvc.perform(post("/api/assets/threats/sync")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ошибка при синхронизации")));
    }
}