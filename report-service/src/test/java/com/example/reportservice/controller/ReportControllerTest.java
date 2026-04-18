package com.example.reportservice.controller;

import com.example.reportservice.TestSecurityConfig;
import com.example.reportservice.client.*;
import com.example.reportservice.dto.*;
import com.example.reportservice.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@WithMockUser(roles = "admin")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private UserServiceClient userClient;

    @MockBean
    private AssetServiceClient assetClient;

    @MockBean
    private TaskServiceClient taskClient;

    @MockBean
    private AuditServiceClient auditClient;

    @Test
    void getOverview_shouldReturnReport() throws Exception {
        OverviewReportDTO dto = new OverviewReportDTO();
        dto.setTotalAssets(100);
        when(reportService.getOverviewReport(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").value(100));
    }

    @Test
    void getAssets_shouldReturnReport() throws Exception {
        AssetsReportDTO dto = new AssetsReportDTO();
        when(reportService.getAssetsReport(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/assets"))
                .andExpect(status().isOk());
    }

    @Test
    void getUsers_shouldReturnReport() throws Exception {
        UsersReportDTO dto = new UsersReportDTO();
        when(reportService.getUsersReport(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/users"))
                .andExpect(status().isOk());
    }

    @Test
    void getSecurity_shouldReturnReport() throws Exception {
        SecurityReportDTO dto = new SecurityReportDTO();
        when(reportService.getSecurityReport(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/security"))
                .andExpect(status().isOk());
    }

    @Test
    void getPerformance_shouldReturnReport() throws Exception {
        PerformanceReportDTO dto = new PerformanceReportDTO();
        when(reportService.getPerformanceReport(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/performance"))
                .andExpect(status().isOk());
    }

    @Test
    void checkServices_shouldReturnStatus() throws Exception {
        mockMvc.perform(get("/api/reports/check-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}