package com.example.reportservice;

import com.example.reportservice.client.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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
class ReportServiceApplicationTests {

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private AssetServiceClient assetServiceClient;

    @MockBean
    private TaskServiceClient taskServiceClient;

    @MockBean
    private AuditServiceClient auditServiceClient;

    @Test
    void contextLoads() {
    }
}