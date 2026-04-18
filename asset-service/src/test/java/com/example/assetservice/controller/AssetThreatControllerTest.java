package com.example.assetservice.controller;

import com.example.assetservice.dto.AddThreatRequest;
import com.example.assetservice.model.Asset;
import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.entity.AssetThreat;
import com.example.assetservice.model.entity.Risk;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.AssetRepository;
import com.example.assetservice.repository.AssetThreatRepository;
import com.example.assetservice.repository.ThreatRepository;
import com.example.assetservice.service.RiskCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetThreatController.class)
@WithMockUser(roles = "admin")
class AssetThreatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssetRepository assetRepository;

    @MockBean
    private ThreatRepository threatRepository;

    @MockBean
    private AssetThreatRepository assetThreatRepository;

    @MockBean
    private RiskCalculationService riskCalculationService;

    private Asset testAsset;
    private Threat testThreat;

    @BeforeEach
    void setUp() {
        testAsset = new Asset();
        testAsset.setId(1L);
        testAsset.setName("Test Asset");
        testAsset.setStatus(AssetStatus.ACTIVE);

        testThreat = new Threat();
        testThreat.setId(10L);
        testThreat.setName("Test Threat");
    }

    @Test
    void getAssetThreats_shouldReturnList() throws Exception {
        AssetThreat at = new AssetThreat();
        at.setId(UUID.randomUUID());
        at.setAsset(testAsset);
        at.setThreat(testThreat);
        at.setProbability(BigDecimal.valueOf(0.5));
        at.setStatus("ACTIVE");

        when(assetRepository.findById(1L)).thenReturn(Optional.of(testAsset));
        when(assetThreatRepository.findByAssetAndStatus(testAsset, "ACTIVE"))
                .thenReturn(List.of(at));

        mockMvc.perform(get("/api/assets/1/threats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].threatId").value(10L))
                .andExpect(jsonPath("$[0].probability").value(0.5));
    }

    @Test
    void addThreat_shouldCreateAndReturn201() throws Exception {
        AddThreatRequest request = new AddThreatRequest();
        request.setThreatId(10L);
        request.setProbability(BigDecimal.valueOf(0.7));
        request.setCustomC(true);
        request.setMitigationEffect(BigDecimal.valueOf(0.2));

        when(assetRepository.findById(1L)).thenReturn(Optional.of(testAsset));
        when(threatRepository.findById(10L)).thenReturn(Optional.of(testThreat));
        when(assetThreatRepository.findByAssetAndThreat(testAsset, testThreat)).thenReturn(Optional.empty());

        AssetThreat saved = new AssetThreat();
        saved.setId(UUID.randomUUID());
        saved.setAsset(testAsset);
        saved.setThreat(testThreat);
        saved.setProbability(request.getProbability());
        saved.setCustomC(true);
        saved.setMitigationEffect(request.getMitigationEffect());
        saved.setAssessmentDate(LocalDate.now());

        when(assetThreatRepository.save(any(AssetThreat.class))).thenReturn(saved);
        when(riskCalculationService.calculateRiskForAsset(testAsset)).thenReturn(new Risk());

        mockMvc.perform(post("/api/assets/1/threats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.threatId").value(10L))
                .andExpect(jsonPath("$.probability").value(0.7))
                .andExpect(jsonPath("$.customC").value(true));

        verify(riskCalculationService, times(1)).calculateRiskForAsset(testAsset);
    }

    @Test
    void addThreat_conflict_shouldReturn409() throws Exception {
        AddThreatRequest request = new AddThreatRequest();
        request.setThreatId(10L);
        request.setProbability(BigDecimal.valueOf(0.7));

        when(assetRepository.findById(1L)).thenReturn(Optional.of(testAsset));
        when(threatRepository.findById(10L)).thenReturn(Optional.of(testThreat));
        when(assetThreatRepository.findByAssetAndThreat(testAsset, testThreat))
                .thenReturn(Optional.of(new AssetThreat()));

        mockMvc.perform(post("/api/assets/1/threats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateThreat_shouldUpdateAndReturn200() throws Exception {
        AddThreatRequest request = new AddThreatRequest();
        request.setProbability(BigDecimal.valueOf(0.9));
        request.setCustomI(true);

        AssetThreat existing = new AssetThreat();
        existing.setId(UUID.randomUUID());
        existing.setAsset(testAsset);
        existing.setThreat(testThreat);
        existing.setProbability(BigDecimal.valueOf(0.5));

        when(assetRepository.findById(1L)).thenReturn(Optional.of(testAsset));
        when(threatRepository.findById(10L)).thenReturn(Optional.of(testThreat));
        when(assetThreatRepository.findByAssetAndThreat(testAsset, testThreat))
                .thenReturn(Optional.of(existing));

        when(assetThreatRepository.save(any(AssetThreat.class))).thenReturn(existing);
        when(riskCalculationService.calculateRiskForAsset(testAsset)).thenReturn(new Risk());

        mockMvc.perform(put("/api/assets/1/threats/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(0.9))
                .andExpect(jsonPath("$.customI").value(true));
    }

    @Test
    @WithMockUser(roles = "admin")
    void removeThreat_asAdmin_shouldSoftDeleteAndReturn204() throws Exception {
        AssetThreat existing = new AssetThreat();
        existing.setId(UUID.randomUUID());
        existing.setAsset(testAsset);
        existing.setThreat(testThreat);
        existing.setStatus("ACTIVE");

        when(assetRepository.findById(1L)).thenReturn(Optional.of(testAsset));
        when(threatRepository.findById(10L)).thenReturn(Optional.of(testThreat));
        when(assetThreatRepository.findByAssetAndThreat(testAsset, testThreat))
                .thenReturn(Optional.of(existing));

        when(assetThreatRepository.save(any(AssetThreat.class))).thenReturn(existing);
        when(riskCalculationService.calculateRiskForAsset(testAsset)).thenReturn(new Risk());

        mockMvc.perform(delete("/api/assets/1/threats/10")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(assetThreatRepository).save(argThat(at -> "RESOLVED".equals(at.getStatus())));
        verify(riskCalculationService).calculateRiskForAsset(testAsset);
    }
}