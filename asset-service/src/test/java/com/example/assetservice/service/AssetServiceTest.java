package com.example.assetservice.service;

import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.model.Asset;
import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.CIA;
import com.example.assetservice.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private RiskCalculationService riskCalculationService;

    @InjectMocks
    private AssetService assetService;

    @Test
    void createAsset_shouldSaveAndReturnAsset() {

        CreateAssetRequest request = new CreateAssetRequest();
        request.setName("Test Asset");
        request.setStatus(AssetStatus.ACTIVE);
        request.setConfidentiality(CIA.MEDIUM);
        request.setIntegrity(CIA.HIGH);
        request.setAvailability(CIA.LOW);

        Jwt jwt = mock(Jwt.class);
        UUID userId = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwt.getClaim("preferred_username")).thenReturn("testuser");

        Asset savedAsset = new Asset();
        savedAsset.setId(1L);
        savedAsset.setName(request.getName());
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);

        Asset result = assetService.createAsset(request, jwt, "127.0.0.1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Asset");
        verify(assetRepository, times(1)).save(any(Asset.class));
        verify(auditEventPublisher, times(1)).publishEvent(any());
        verify(riskCalculationService, times(1)).calculateRiskForAsset(any());
    }

    @Test
    void getAssetById_shouldThrowWhenNotFound() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> assetService.getAssetById(99L));
    }
}