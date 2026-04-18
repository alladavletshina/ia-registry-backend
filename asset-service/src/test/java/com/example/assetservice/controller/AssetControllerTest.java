package com.example.assetservice.controller;

import com.example.assetservice.dto.AssetResponse;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.model.Asset;
import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import com.example.assetservice.service.AssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetService assetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "admin")
    void getAssetById_shouldReturn200() throws Exception {
        AssetResponse response = new AssetResponse();
        response.setId(1L);
        response.setName("Test");

        when(assetService.getAssetById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/assets/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "user")
    void getAllAssets_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    void createAsset_shouldReturn201() throws Exception {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("New Asset");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.MEDIUM);
        request.setIntegrity(CIA.MEDIUM);
        request.setAvailability(CIA.MEDIUM);

        Asset createdAsset = new Asset();
        createdAsset.setId(10L);
        createdAsset.setName(request.getName());

        when(assetService.createAsset(any(CreateAssetRequest.class), any(), anyString()))
                .thenReturn(createdAsset);

        mockMvc.perform(post("/api/assets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("New Asset"));
    }

    @Test
    @WithMockUser(roles = "admin")
    void updateAsset_shouldReturn200() throws Exception {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Updated Asset");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.HIGH);
        request.setIntegrity(CIA.HIGH);
        request.setAvailability(CIA.HIGH);

        AssetResponse response = new AssetResponse();
        response.setId(1L);
        response.setName("Updated Asset");

        when(assetService.updateAsset(eq(1L), any(CreateAssetRequest.class), any(), anyString()))
                .thenReturn(response);

        mockMvc.perform(put("/api/assets/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Asset"));
    }

    @Test
    @WithMockUser(roles = "admin")
    void deleteAsset_shouldReturn204() throws Exception {
        doNothing().when(assetService).deleteAsset(eq(1L), any(), anyString());

        mockMvc.perform(delete("/api/assets/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(assetService, times(1)).deleteAsset(eq(1L), any(), anyString());
    }

    @Test
    @WithMockUser(roles = "user")
    void patchAsset_shouldReturn200() throws Exception {
        Map<String, Object> updates = Map.of("status", "ARCHIVED");

        AssetResponse response = new AssetResponse();
        response.setId(1L);
        response.setStatus(AssetStatus.ARCHIVED);

        when(assetService.patchAsset(eq(1L), anyMap(), any(), anyString()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/assets/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}