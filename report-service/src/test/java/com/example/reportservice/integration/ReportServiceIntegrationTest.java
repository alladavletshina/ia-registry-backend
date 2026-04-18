package com.example.reportservice.integration;

import com.example.reportservice.TestSecurityConfig;
import com.example.reportservice.client.*;
import com.example.reportservice.dto.*;
import com.example.reportservice.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "services.user-service.url=http://localhost:9999",
        "services.asset-service.url=http://localhost:9999",
        "services.task-service.url=http://localhost:9999",
        "services.audit-service.url=http://localhost:9999"
})
@Import(TestSecurityConfig.class)
class ReportServiceIntegrationTest {

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private AssetServiceClient assetServiceClient;

    @MockBean
    private TaskServiceClient taskServiceClient;

    @MockBean
    private AuditServiceClient auditServiceClient;

    @Autowired
    private ReportService reportService;

    @Test
    void getOverviewReport_shouldAggregateDataFromMultipleServices() {
        AssetDTO asset = new AssetDTO();
        asset.setId(1L);
        asset.setGroupName("Аппаратные активы");
        asset.setConfidentiality("HIGH");
        when(assetServiceClient.getAllAssets()).thenReturn(List.of(asset));

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID());
        when(userServiceClient.getAllUsers()).thenReturn(List.of(user));

        TaskStatsDTO stats = new TaskStatsDTO();
        stats.setPending(5);
        when(taskServiceClient.getTaskStats()).thenReturn(stats);

        OverviewReportDTO report = reportService.getOverviewReport("month");

        assertThat(report.getTotalAssets()).isEqualTo(1);
        assertThat(report.getTotalUsers()).isEqualTo(1);
        assertThat(report.getPendingReviews()).isEqualTo(5);
        assertThat(report.getHighRiskAssets()).isEqualTo(1);
    }

    @Test
    void getUsersReport_shouldProcessAuditEvents() {
        AuditEventDTO event = new AuditEventDTO();
        event.setAction("LOGIN");
        event.setUsername("admin");
        event.setSeverity("SUCCESS");
        event.setTimestamp(LocalDateTime.now().minusDays(1).toString());
        when(auditServiceClient.getReportData(anyString(), anyString())).thenReturn(List.of(event));

        UsersReportDTO report = reportService.getUsersReport("week");

        assertThat(report.getActivityByRole()).isNotEmpty();
        assertThat(report.getDailyActivity()).hasSize(7);
    }

    @Test
    void getSecurityReport_shouldReturnRiskDistribution() {
        AuditEventDTO danger = new AuditEventDTO();
        danger.setSeverity("DANGER");
        danger.setAction("UNAUTHORIZED_ACCESS");
        danger.setTimestamp(LocalDateTime.now().toString());

        when(auditServiceClient.getReportData(anyString(), anyString()))
                .thenReturn(List.of(danger));

        SecurityReportDTO report = reportService.getSecurityReport("month");

        assertThat(report.getRiskDistribution()).hasSize(4);
        long highRiskCount = report.getRiskDistribution().stream()
                .filter(rc -> "Высокий риск".equals(rc.getName()))
                .mapToInt(SecurityReportDTO.RiskCount::getValue)
                .sum();
        assertThat(highRiskCount).isEqualTo(1);
    }
}