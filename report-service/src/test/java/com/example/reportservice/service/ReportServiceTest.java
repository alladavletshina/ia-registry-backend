package com.example.reportservice.service;

import com.example.reportservice.TestSecurityConfig;
import com.example.reportservice.client.*;
import com.example.reportservice.config.PerformanceProperties;
import com.example.reportservice.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Import(TestSecurityConfig.class)
class ReportServiceTest {

    @Mock private UserServiceClient userClient;
    @Mock private AssetServiceClient assetClient;
    @Mock private TaskServiceClient taskClient;
    @Mock private AuditServiceClient auditClient;
    @Mock private RestTemplate restTemplate;
    @Mock private PerformanceProperties performanceProperties;

    @InjectMocks
    private ReportService reportService;

    private List<AssetDTO> mockAssets;
    private List<UserDTO> mockUsers;
    private TaskStatsDTO mockTaskStats;
    private List<AuditEventDTO> mockAuditEvents;

    @BeforeEach
    void setUp() {
        // Подготовка мок-данных
        AssetDTO asset = new AssetDTO();
        asset.setId(1L);
        asset.setName("Test Asset");
        asset.setGroupName("Аппаратные активы");
        asset.setStatus("ACTIVE");
        asset.setConfidentiality("HIGH");
        asset.setIntegrity("MEDIUM");
        asset.setAvailability("LOW");
        asset.setCreatedAt(LocalDateTime.now().minusDays(5));
        mockAssets = List.of(asset);

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole("admin");
        mockUsers = List.of(user);

        mockTaskStats = new TaskStatsDTO();
        mockTaskStats.setTotal(10);
        mockTaskStats.setPending(2);
        mockTaskStats.setInProgress(3);
        mockTaskStats.setCompleted(4);
        mockTaskStats.setOverdue(1);

        AuditEventDTO event = new AuditEventDTO();
        event.setAction("LOGIN");
        event.setUsername("admin");
        event.setSeverity("SUCCESS");
        event.setTimestamp(LocalDateTime.now().minusDays(1).toString());
        mockAuditEvents = List.of(event);
    }

    @Test
    void getOverviewReport_shouldReturnReport() {
        when(assetClient.getAllAssets()).thenReturn(mockAssets);
        when(userClient.getAllUsers()).thenReturn(mockUsers);
        when(taskClient.getTaskStats()).thenReturn(mockTaskStats);

        OverviewReportDTO report = reportService.getOverviewReport("month");

        assertThat(report.getTotalAssets()).isEqualTo(1);
        assertThat(report.getTotalUsers()).isEqualTo(1);
        assertThat(report.getPendingReviews()).isEqualTo(2);
        assertThat(report.getHighRiskAssets()).isEqualTo(1);
        assertThat(report.getCategoryDistribution()).isNotEmpty();
        assertThat(report.getCiaDistribution()).hasSize(3);
    }

    @Test
    void getOverviewReport_whenClientsFail_shouldReturnEmptyData() {
        when(assetClient.getAllAssets()).thenThrow(new RuntimeException("Connection error"));
        when(userClient.getAllUsers()).thenThrow(new RuntimeException("Connection error"));
        when(taskClient.getTaskStats()).thenThrow(new RuntimeException("Connection error"));

        OverviewReportDTO report = reportService.getOverviewReport("month");

        assertThat(report.getTotalAssets()).isEqualTo(0);
        assertThat(report.getTotalUsers()).isEqualTo(0);
        assertThat(report.getPendingReviews()).isEqualTo(0);
        assertThat(report.getHighRiskAssets()).isEqualTo(0);
        assertThat(report.getCategoryDistribution()).isEmpty();
        assertThat(report.getCiaDistribution()).hasSize(3);
    }

    @Test
    void getAssetsReport_shouldReturnReport() {
        when(assetClient.getAllAssets()).thenReturn(mockAssets);

        AssetsReportDTO report = reportService.getAssetsReport("month");

        assertThat(report.getByCategory()).isNotEmpty();
        assertThat(report.getByStatus()).isNotEmpty();
        assertThat(report.getByConfidentiality()).isNotEmpty();
        assertThat(report.getGrowthTrend()).isNotNull();
    }

    @Test
    void getUsersReport_shouldReturnReport() {
        when(auditClient.getReportData(anyString(), anyString())).thenReturn(mockAuditEvents);

        UsersReportDTO report = reportService.getUsersReport("month");

        assertThat(report.getActivityByRole()).hasSize(2);
        assertThat(report.getDailyActivity()).hasSize(7);
        assertThat(report.getTopUsers()).isNotEmpty();
    }

    @Test
    void getSecurityReport_shouldReturnReport() {
        when(auditClient.getReportData(anyString(), anyString())).thenReturn(mockAuditEvents);

        SecurityReportDTO report = reportService.getSecurityReport("month");

        assertThat(report.getRiskDistribution()).hasSize(4);
        assertThat(report.getAuditEvents()).isNotEmpty();
        assertThat(report.getComplianceStatus()).isNotNull();
    }

    @Test
    void getPerformanceReport_shouldReturnReport() {
        when(auditClient.getReportData(anyString(), anyString())).thenReturn(mockAuditEvents);
        when(performanceProperties.getServices()).thenReturn(List.of());

        PerformanceReportDTO report = reportService.getPerformanceReport("month");

        assertThat(report.getUptime()).isZero();
        assertThat(report.getAvgResponseTime()).isZero();
        assertThat(report.getErrors()).isNotNull();
    }
}
