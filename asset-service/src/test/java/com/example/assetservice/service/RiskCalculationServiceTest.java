package com.example.assetservice.service;

import com.example.assetservice.model.Asset;
import com.example.assetservice.model.entity.AssetThreat;
import com.example.assetservice.model.entity.Risk;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.AssetThreatRepository;
import com.example.assetservice.repository.RiskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskCalculationServiceTest {

    @Mock
    private AssetThreatRepository assetThreatRepository;

    @Mock
    private RiskRepository riskRepository;

    @InjectMocks
    private RiskCalculationService riskCalculationService;

    private Asset createAsset(Long id, BigDecimal value, int weightC, int weightI, int weightA) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setValue(value);
        asset.setWeightC(weightC);
        asset.setWeightI(weightI);
        asset.setWeightA(weightA);
        asset.setName("Test Asset");
        return asset;
    }

    private Threat createThreat(Long id, boolean confidentiality, boolean integrity, boolean availability) {
        Threat threat = new Threat();
        threat.setId(id);
        threat.setName("Threat " + id);
        threat.setConfidentiality(confidentiality);
        threat.setIntegrity(integrity);
        threat.setAvailability(availability);
        return threat;
    }

    private AssetThreat createAssetThreat(Asset asset, Threat threat, BigDecimal probability,
                                          Boolean customC, Boolean customI, Boolean customA,
                                          BigDecimal mitigationEffect) {
        AssetThreat at = new AssetThreat();
        at.setAsset(asset);
        at.setThreat(threat);
        at.setProbability(probability);
        at.setCustomC(customC);
        at.setCustomI(customI);
        at.setCustomA(customA);
        at.setMitigationEffect(mitigationEffect != null ? mitigationEffect : BigDecimal.ZERO);
        at.setStatus("ACTIVE");
        return at;
    }

    @Test
    void calculateRiskForAsset_noThreats_shouldReturnZeroRisk() {

        Asset asset = createAsset(1L, new BigDecimal("100000"), 1, 1, 1);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of());

        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(BigDecimal.ZERO);
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(BigDecimal.ZERO);
        ArgumentCaptor<Risk> riskCaptor = ArgumentCaptor.forClass(Risk.class);
        verify(riskRepository).save(riskCaptor.capture());
        assertThat(riskCaptor.getValue().getCalculationDetails()).contains("Нет привязанных активных угроз");
    }

    @Test
    void calculateRiskForAsset_singleThreat_fullDamage() {

        Asset asset = createAsset(1L, new BigDecimal("100000"), 1, 1, 1);
        Threat threat = createThreat(10L, true, true, true);
        BigDecimal probability = BigDecimal.ONE;
        AssetThreat assetThreat = createAssetThreat(asset, threat, probability, null, null, null, BigDecimal.ZERO);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of(assetThreat));

        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(BigDecimal.valueOf(100000));
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    void calculateRiskForAsset_singleThreat_partialProbability() {

        Asset asset = createAsset(1L, new BigDecimal("100000"), 1, 1, 1);
        Threat threat = createThreat(10L, true, false, false);
        BigDecimal probability = new BigDecimal("0.5");
        AssetThreat assetThreat = createAssetThreat(asset, threat, probability, null, null, null, BigDecimal.ZERO);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of(assetThreat));

        BigDecimal expected = new BigDecimal("100000")
                .multiply(new BigDecimal("0.5"))
                .multiply(BigDecimal.ONE)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(expected);
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(expected);
    }

    @Test
    void calculateRiskForAsset_withMitigationEffect() {

        Asset asset = createAsset(1L, new BigDecimal("100000"), 2, 1, 1);
        Threat threat = createThreat(10L, true, true, false);
        BigDecimal probability = new BigDecimal("0.8");
        BigDecimal mitigation = new BigDecimal("0.3");
        AssetThreat assetThreat = createAssetThreat(asset, threat, probability, null, null, null, mitigation);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of(assetThreat));

        BigDecimal expected = new BigDecimal("100000")
                .multiply(new BigDecimal("0.56"))
                .multiply(BigDecimal.valueOf(3))
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);

        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(expected);
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(expected);
    }

    @Test
    void calculateRiskForAsset_customFlagsOverrideThreatFlags() {

        Asset asset = createAsset(1L, new BigDecimal("50000"), 1, 2, 3);
        Threat threat = createThreat(10L, true, false, true);
        AssetThreat assetThreat = createAssetThreat(asset, threat, BigDecimal.ONE,
                false, true, false, BigDecimal.ZERO);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of(assetThreat));

        BigDecimal expected = new BigDecimal("50000")
                .multiply(BigDecimal.valueOf(2))
                .divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);

        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(expected);
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(expected);
    }

    @Test
    void calculateRiskForAsset_multipleThreats_summedDamage() {

        Asset asset = createAsset(1L, new BigDecimal("100000"), 1, 1, 1);
        Threat threat1 = createThreat(1L, true, false, false);
        Threat threat2 = createThreat(2L, false, true, false);
        Threat threat3 = createThreat(3L, false, false, true);

        AssetThreat at1 = createAssetThreat(asset, threat1, new BigDecimal("0.5"), null, null, null, BigDecimal.ZERO);
        AssetThreat at2 = createAssetThreat(asset, threat2, new BigDecimal("0.3"), null, null, null, BigDecimal.ZERO);
        AssetThreat at3 = createAssetThreat(asset, threat3, new BigDecimal("0.2"), null, null, null, BigDecimal.ZERO);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of(at1, at2, at3));

        BigDecimal expected = new BigDecimal("33333.34");
        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(expected);
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(expected);
    }

    @Test
    void calculateRiskForAsset_assetValueZero_shouldReturnZero() {

        Asset asset = createAsset(1L, BigDecimal.ZERO, 1, 1, 1);
        Threat threat = createThreat(10L, true, true, true);
        AssetThreat assetThreat = createAssetThreat(asset, threat, BigDecimal.ONE, null, null, null, BigDecimal.ZERO);
        when(assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")).thenReturn(List.of(assetThreat));

        Risk savedRisk = new Risk();
        savedRisk.setCalculatedRisk(BigDecimal.ZERO);
        when(riskRepository.save(any(Risk.class))).thenReturn(savedRisk);

        Risk result = riskCalculationService.calculateRiskForAsset(asset);

        assertThat(result.getCalculatedRisk()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}