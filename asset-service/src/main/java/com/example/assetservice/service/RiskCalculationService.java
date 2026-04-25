package com.example.assetservice.service;

import com.example.assetservice.model.Asset;
import com.example.assetservice.model.entity.AssetThreat;
import com.example.assetservice.model.entity.Risk;
import com.example.assetservice.repository.AssetThreatRepository;
import com.example.assetservice.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationService {

    private final AssetThreatRepository assetThreatRepository;
    private final RiskRepository riskRepository;

    /**
     * Рассчитывает интегральный риск для актива по формулам (1) и (2) из диплома
     * и сохраняет результат в историю.
     *
     * @param asset актив, для которого рассчитывается риск
     * @return сохранённая запись риска
     */
    @Transactional
    public Risk calculateRiskForAsset(Asset asset) {
        List<AssetThreat> activeThreats = assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE");

        if (activeThreats.isEmpty()) {
            Risk risk = new Risk();
            risk.setAsset(asset);
            risk.setCalculatedRisk(BigDecimal.ZERO);
            risk.setCalculationDetails("Нет привязанных активных угроз");
            return riskRepository.save(risk);
        }

        BigDecimal totalRisk = BigDecimal.ZERO;
        StringBuilder details = new StringBuilder("Учтены угрозы:\n");

        for (AssetThreat at : activeThreats) {
            BigDecimal damage = calculateDamage(at);
            totalRisk = totalRisk.add(damage);
            details.append(String.format("- %s (id=%d): ущерб = %.2f руб.\n",
                    at.getThreat().getName(), at.getThreat().getId(), damage));
        }

        Risk risk = new Risk();
        risk.setAsset(asset);
        risk.setCalculatedRisk(totalRisk);
        risk.setCalculationDetails(details.toString());
        return riskRepository.save(risk);
    }

    /**
     * Расчёт ущерба от одной угрозы по формуле (1)
     * D = V * P_eff * (w_c*c + w_i*i + w_a*a) / (w_c + w_i + w_a)
     */
    private BigDecimal calculateDamage(AssetThreat at) {
        Asset asset = at.getAsset();

        BigDecimal effectiveProbability = at.getProbability()
                .multiply(BigDecimal.ONE.subtract(at.getMitigationEffect()));

        boolean c = at.getCustomC() != null ? at.getCustomC() : at.getThreat().isConfidentiality();
        boolean i = at.getCustomI() != null ? at.getCustomI() : at.getThreat().isIntegrity();
        boolean a = at.getCustomA() != null ? at.getCustomA() : at.getThreat().isAvailability();

        int wC = asset.getWeightC();
        int wI = asset.getWeightI();
        int wA = asset.getWeightA();

        int denominator = wC + wI + wA;
        if (denominator == 0) return BigDecimal.ZERO;

        BigDecimal numerator = BigDecimal.valueOf(wC * (c ? 1 : 0))
                .add(BigDecimal.valueOf(wI * (i ? 1 : 0)))
                .add(BigDecimal.valueOf(wA * (a ? 1 : 0)));

        return asset.getValue()
                .multiply(effectiveProbability)
                .multiply(numerator)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}