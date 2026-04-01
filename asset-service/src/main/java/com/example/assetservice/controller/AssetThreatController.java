package com.example.assetservice.controller;

import com.example.assetservice.dto.AddThreatRequest;
import com.example.assetservice.dto.AssetThreatDto;
import com.example.assetservice.model.Asset;
import com.example.assetservice.model.entity.AssetThreat;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.AssetThreatRepository;
import com.example.assetservice.repository.AssetRepository;
import com.example.assetservice.repository.ThreatRepository;
import com.example.assetservice.service.RiskCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets/{assetId}/threats")
@RequiredArgsConstructor
@Tag(name = "Связи актив-угроза", description = "Управление угрозами для конкретного актива")
public class AssetThreatController {

    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final AssetThreatRepository assetThreatRepository;
    private final RiskCalculationService riskCalculationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'user')")
    @Operation(summary = "Получить все угрозы, привязанные к активу")
    public List<AssetThreatDto> getAssetThreats(@PathVariable Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив не найден"));
        return assetThreatRepository.findByAssetAndStatus(asset, "ACTIVE")
                .stream()
                .map(AssetThreatDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'user')")
    @Operation(summary = "Привязать угрозу к активу")
    public ResponseEntity<AssetThreatDto> addThreat(
            @PathVariable Long assetId,
            @RequestBody AddThreatRequest request) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив не найден"));
        Threat threat = threatRepository.findById(request.getThreatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Угроза не найдена"));

        // Проверяем, нет ли уже такой активной связи
        if (assetThreatRepository.findByAssetAndThreat(asset, threat).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Угроза уже привязана к активу");
        }

        AssetThreat at = new AssetThreat();
        at.setAsset(asset);
        at.setThreat(threat);
        at.setProbability(request.getProbability());
        at.setCustomC(request.getCustomC());
        at.setCustomI(request.getCustomI());
        at.setCustomA(request.getCustomA());
        at.setMitigationEffect(request.getMitigationEffect() != null ? request.getMitigationEffect() : BigDecimal.ZERO);
        at.setAssessmentDate(LocalDate.now());

        AssetThreat saved = assetThreatRepository.save(at);

        // Пересчитываем риск после добавления угрозы
        riskCalculationService.calculateRiskForAsset(asset);

        return ResponseEntity.status(HttpStatus.CREATED).body(AssetThreatDto.fromEntity(saved));
    }

    @PutMapping("/{threatId}")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    @Operation(summary = "Обновить параметры привязанной угрозы")
    public ResponseEntity<AssetThreatDto> updateThreat(
            @PathVariable Long assetId,
            @PathVariable Long threatId,
            @RequestBody AddThreatRequest request) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив не найден"));
        Threat threat = threatRepository.findById(threatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Угроза не найдена"));

        AssetThreat at = assetThreatRepository.findByAssetAndThreat(asset, threat)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Связь не найдена"));

        at.setProbability(request.getProbability());
        at.setCustomC(request.getCustomC());
        at.setCustomI(request.getCustomI());
        at.setCustomA(request.getCustomA());
        at.setMitigationEffect(request.getMitigationEffect() != null ? request.getMitigationEffect() : BigDecimal.ZERO);
        at.setAssessmentDate(LocalDate.now());

        AssetThreat updated = assetThreatRepository.save(at);

        // Пересчитываем риск после изменения
        riskCalculationService.calculateRiskForAsset(asset);

        return ResponseEntity.ok(AssetThreatDto.fromEntity(updated));
    }

    @DeleteMapping("/{threatId}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Отвязать угрозу от актива")
    public ResponseEntity<Void> removeThreat(
            @PathVariable Long assetId,
            @PathVariable Long threatId) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив не найден"));
        Threat threat = threatRepository.findById(threatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Угроза не найдена"));

        AssetThreat at = assetThreatRepository.findByAssetAndThreat(asset, threat)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Связь не найдена"));

        at.setStatus("RESOLVED"); // мягкое удаление
        assetThreatRepository.save(at);

        // Пересчитываем риск после удаления угрозы
        riskCalculationService.calculateRiskForAsset(asset);

        return ResponseEntity.noContent().build();
    }
}
