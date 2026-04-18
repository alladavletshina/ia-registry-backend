package com.example.assetservice.integration;

import com.example.assetservice.dto.AssetResponse;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.model.Asset;
import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import com.example.assetservice.model.entity.AssetGroup;
import com.example.assetservice.repository.AssetGroupRepository;
import com.example.assetservice.repository.AssetRepository;
import com.example.assetservice.service.AssetService;
import com.example.assetservice.service.AuditEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AssetServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration");
    }

    @MockBean
    private AuditEventPublisher auditEventPublisher;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetRepository assetRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AssetGroupRepository assetGroupRepository;

    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        UUID userId = UUID.randomUUID();
        when(mockJwt.getSubject()).thenReturn(userId.toString());
        when(mockJwt.getClaim("preferred_username")).thenReturn("testuser");
    }

    @Test
    void createAsset_shouldPersistAndReturnAsset() {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Integration Test Asset");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.HIGH);
        request.setIntegrity(CIA.MEDIUM);
        request.setAvailability(CIA.LOW);
        request.setValue(new BigDecimal("50000"));
        request.setWeightC(2);
        request.setWeightI(1);
        request.setWeightA(3);
        request.setLegalStatus("Confidential");

        Asset created = assetService.createAsset(request, mockJwt, "127.0.0.1");

        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("Integration Test Asset");
        assertThat(created.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(created.getValue()).isEqualByComparingTo("50000");

        Asset found = assetRepository.findById(created.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo(created.getName());
    }

    @Test
    void getAssetById_shouldReturnCorrectAsset() {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Asset for GetById");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.LOW);
        request.setIntegrity(CIA.LOW);
        request.setAvailability(CIA.LOW);
        Asset asset = assetService.createAsset(request, mockJwt, "127.0.0.1");

        AssetResponse response = assetService.getAssetById(asset.getId());

        assertThat(response.getId()).isEqualTo(asset.getId());
        assertThat(response.getName()).isEqualTo("Asset for GetById");
        assertThat(response.getStatus()).isEqualTo(AssetStatus.ACTIVE);
    }

    @Test
    void updateAsset_shouldModifyFields() {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Before Update");
        request.setStatus(AssetStatus.DRAFT);
        request.setConfidentiality(CIA.LOW);
        request.setIntegrity(CIA.LOW);
        request.setAvailability(CIA.LOW);
        Asset asset = assetService.createAsset(request, mockJwt, "127.0.0.1");

        CreateAssetRequest updateRequest = new CreateAssetRequest();
        updateRequest.setName("After Update");
        updateRequest.setStatus(AssetStatus.ACTIVE);
        updateRequest.setConfidentiality(CIA.HIGH);
        updateRequest.setIntegrity(CIA.HIGH);
        updateRequest.setAvailability(CIA.HIGH);
        updateRequest.setValue(new BigDecimal("100000"));

        AssetResponse updated = assetService.updateAsset(asset.getId(), updateRequest, mockJwt, "127.0.0.1");

        assertThat(updated.getName()).isEqualTo("After Update");
        assertThat(updated.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(updated.getConfidentiality()).isEqualTo(CIA.HIGH);
        assertThat(updated.getValue()).isEqualByComparingTo("100000");
    }

    @Test
    void deleteAsset_shouldRemoveFromDatabase() {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("To Be Deleted");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.LOW);
        request.setIntegrity(CIA.LOW);
        request.setAvailability(CIA.LOW);
        Asset asset = assetService.createAsset(request, mockJwt, "127.0.0.1");
        Long id = asset.getId();

        assetService.deleteAsset(id, mockJwt, "127.0.0.1");

        assertThat(assetRepository.findById(id)).isEmpty();
    }

    @Test
    void createAsset_withGroup_shouldLinkGroup() {
        AssetGroup group = new AssetGroup();
        group.setName("Test Group");
        group.setCode("TEST_GROUP");
        group.setDescription("Integration test group");
        AssetGroup savedGroup = assetGroupRepository.save(group);

        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Asset With Group");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.MEDIUM);
        request.setIntegrity(CIA.MEDIUM);
        request.setAvailability(CIA.MEDIUM);
        request.setGroupId(savedGroup.getId());

        Asset created = assetService.createAsset(request, mockJwt, "127.0.0.1");

        assertThat(created.getGroup()).isNotNull();
        assertThat(created.getGroup().getId()).isEqualTo(savedGroup.getId());
        assertThat(created.getGroup().getName()).isEqualTo("Test Group");
    }

    @Test
    void getLatestRisk_shouldReturnRiskAfterCalculation() {
        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Risk Test Asset");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.CRITICAL);
        request.setIntegrity(CIA.CRITICAL);
        request.setAvailability(CIA.CRITICAL);
        // Исправлено: убрано подчёркивание из строки
        request.setValue(BigDecimal.valueOf(1_000_000));

        Asset asset = assetService.createAsset(request, mockJwt, "127.0.0.1");

        var risks = assetService.getLatestRisk(asset.getId());

        assertThat(risks).isNotEmpty();
        assertThat(risks.get(0).getCalculatedRisk()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(risks.get(0).getCalculationDetails()).contains("Нет привязанных активных угроз");
    }
}