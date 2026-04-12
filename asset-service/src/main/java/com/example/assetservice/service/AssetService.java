package com.example.assetservice.service;

import com.example.assetservice.dto.AssetResponse;
import com.example.assetservice.dto.AuditEventDto;
import com.example.assetservice.model.Asset;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.model.AssetStatus;
import com.example.assetservice.model.entity.AssetGroup;
import com.example.assetservice.model.entity.Risk;
import com.example.assetservice.repository.AssetGroupRepository;
import com.example.assetservice.repository.AssetRepository;
import com.example.assetservice.repository.RiskRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final AssetGroupRepository assetGroupRepository;
    private final RiskCalculationService riskCalculationService;
    private final RiskRepository riskRepository;

    @Transactional
    public Asset createAsset(CreateAssetRequest request, Jwt jwt, String clientIp) {
        Asset asset = new Asset();
        asset.setName(request.getName());
        asset.setOwnerId(request.getOwnerId());
        asset.setStatus(request.getStatus());
        asset.setConfidentiality(request.getConfidentiality());
        asset.setIntegrity(request.getIntegrity());
        asset.setAvailability(request.getAvailability());
        asset.setLastReview(request.getLastReview());
        asset.setDescription(request.getDescription());
        asset.setLocation(request.getLocation());
        asset.setTags(request.getTags());

        // Новые поля для расчетов
        asset.setValue(request.getValue());
        asset.setWeightC(request.getWeightC() != null ? request.getWeightC() : 1);
        asset.setWeightI(request.getWeightI() != null ? request.getWeightI() : 1);
        asset.setWeightA(request.getWeightA() != null ? request.getWeightA() : 1);
        asset.setLegalStatus(request.getLegalStatus());

        if (request.getGroupId() != null) {
            AssetGroup group = assetGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Группа активов не найдена"));
            asset.setGroup(group);
        }

        Asset saved = assetRepository.save(asset);

        // Пересчёт риска (пока угроз нет, риск будет 0, но создаётся запись в risk_history)
        riskCalculationService.calculateRiskForAsset(saved);

        /* Отправка события аудита */
        AuditEventDto event = new AuditEventDto();
        event.setUserId(UUID.fromString(jwt.getSubject()));
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("CREATE_ASSET");
        event.setDetails(String.format("Создан актив: %s (id=%d)", saved.getName(), saved.getId()));
        event.setIp(clientIp);
        event.setSeverity("SUCCESS");
        event.setServiceName("asset-service");
        event.setObjectId(String.valueOf(saved.getId()));
        event.setObjectType("Asset");

        auditEventPublisher.publishEvent(event);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AssetResponse mapToResponse(Asset asset) {
        AssetResponse response = new AssetResponse();
        response.setId(asset.getId());
        response.setName(asset.getName());
        response.setOwnerId(asset.getOwnerId());
        response.setStatus(asset.getStatus());
        response.setConfidentiality(asset.getConfidentiality());
        response.setIntegrity(asset.getIntegrity());
        response.setAvailability(asset.getAvailability());
        response.setLastReview(asset.getLastReview());
        response.setDescription(asset.getDescription());
        response.setLocation(asset.getLocation());
        response.setTags(asset.getTags());
        response.setCreatedAt(asset.getCreatedAt());
        response.setUpdatedAt(asset.getUpdatedAt());

        // Новые поля для расчетов
        response.setValue(asset.getValue());
        response.setWeightC(asset.getWeightC());
        response.setWeightI(asset.getWeightI());
        response.setWeightA(asset.getWeightA());
        response.setLegalStatus(asset.getLegalStatus());
        if (asset.getGroup() != null) {
            response.setGroupId(asset.getGroup().getId());
            response.setGroupName(asset.getGroup().getName());
        }

        // Получаем последний риск для актива
        List<Risk> risks = riskRepository.findByAssetOrderByCalculationDateDesc(asset);
        if (!risks.isEmpty()) {
            response.setLatestRisk(risks.get(0).getCalculatedRisk());
        }
        return response;
    }

    public AssetResponse getAssetById(long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Asset not found with id: " + id));
        return mapToResponse(asset);
    }

    @Transactional
    public AssetResponse updateAsset(long id, @Valid CreateAssetRequest request, Jwt jwt, String clientIp) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Актив с id " + id + " не найден"));

        // Обновление существующих полей
        asset.setName(request.getName());
        asset.setOwnerId(request.getOwnerId());
        asset.setStatus(request.getStatus());
        asset.setConfidentiality(request.getConfidentiality());
        asset.setIntegrity(request.getIntegrity());
        asset.setAvailability(request.getAvailability());
        asset.setLastReview(request.getLastReview());
        asset.setDescription(request.getDescription());
        asset.setLocation(request.getLocation());
        asset.setTags(request.getTags());

        // Новые поля для расчетов
        asset.setValue(request.getValue());
        asset.setWeightC(request.getWeightC() != null ? request.getWeightC() : 1);
        asset.setWeightI(request.getWeightI() != null ? request.getWeightI() : 1);
        asset.setWeightA(request.getWeightA() != null ? request.getWeightA() : 1);
        asset.setLegalStatus(request.getLegalStatus());

        if (request.getGroupId() != null) {
            AssetGroup group = assetGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Группа активов не найдена"));
            asset.setGroup(group);
        } else {
            asset.setGroup(null);
        }

        asset.setUpdatedAt(LocalDateTime.now());

        Asset saved = assetRepository.save(asset);

        // Пересчёт риска после обновления
        riskCalculationService.calculateRiskForAsset(saved);

        // Отправка события аудита
        AuditEventDto event = new AuditEventDto();
        event.setUserId(UUID.fromString(jwt.getSubject()));
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("UPDATE_ASSET");
        event.setDetails(String.format("Изменен актив: %s (id=%d)", saved.getName(), saved.getId()));
        event.setIp(clientIp);
        event.setSeverity("WARNING");
        event.setServiceName("asset-service");
        event.setObjectId(String.valueOf(saved.getId()));
        event.setObjectType("Asset");

        auditEventPublisher.publishEvent(event);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAsset(Long id, Jwt jwt, String clientIp) {
        if (!assetRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив с id " + id + " не найден");
        }

        Asset asset = assetRepository.getAssetById(id);

        /* Отправка события аудита */
        AuditEventDto event = new AuditEventDto();
        event.setUserId(UUID.fromString(jwt.getSubject()));
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("DELETE_ASSET");
        event.setDetails(String.format("Удален актив: %s (id=%d)", asset.getName(), asset.getId()));
        event.setIp(clientIp);
        event.setSeverity("DANGER");
        event.setServiceName("asset-service");
        event.setObjectId(String.valueOf(asset.getId()));
        event.setObjectType("Asset");

        assetRepository.deleteById(id);
        auditEventPublisher.publishEvent(event);
    }

    public List<AssetResponse> getAssetByOwnerId(String ownerId) {

        return assetRepository.findAllByOwnerId(ownerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    public List<AssetGroup> getAllGroups() {
        return assetGroupRepository.findAll();
    }

    public List<Risk> getLatestRisk(long id) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Risk> risks = riskRepository.findByAssetOrderByCalculationDateDesc(asset);

        return risks;
    }

    @Transactional(readOnly = true)
    public Page<AssetResponse> searchAssetsByName(String query, Pageable pageable) {
        Page<Asset> assets = assetRepository.findByNameContainingIgnoreCase(query, pageable);
        return assets.map(this::mapToResponse);
    }

    @Transactional
    public AssetResponse patchAsset(long id, Map<String, Object> updates, Jwt jwt, String clientIp) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив не найден"));

        if (updates.containsKey("status")) {
            String statusStr = (String) updates.get("status");
            AssetStatus newStatus = AssetStatus.valueOf(statusStr);
            asset.setStatus(newStatus);
        }
        if (updates.containsKey("name")) {
            asset.setName((String) updates.get("name"));
        }
        if (updates.containsKey("description")) {
            asset.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("ownerId")) {
            asset.setOwnerId((String) updates.get("ownerId"));
        }
        if (updates.containsKey("value")) {
            asset.setValue(new BigDecimal(updates.get("value").toString()));
        }
        if (updates.containsKey("groupId")) {
            String groupIdStr = (String) updates.get("groupId");
            if (groupIdStr != null && !groupIdStr.isEmpty()) {
                AssetGroup group = assetGroupRepository.findById(UUID.fromString(groupIdStr))
                        .orElseThrow(() -> new RuntimeException("Группа не найдена"));
                asset.setGroup(group);
            } else {
                asset.setGroup(null);
            }
        }

        asset.setUpdatedAt(LocalDateTime.now());
        Asset saved = assetRepository.save(asset);

        // Отправляем аудит
        AuditEventDto event = new AuditEventDto();
        event.setUserId(UUID.fromString(jwt.getSubject()));
        event.setUsername(jwt.getClaim("preferred_username"));
        event.setAction("UPDATE_ASSET");
        event.setDetails(String.format("Частично обновлен актив: %s (id=%d)", saved.getName(), saved.getId()));
        event.setIp(clientIp);
        event.setSeverity("WARNING");
        event.setServiceName("asset-service");
        event.setObjectId(String.valueOf(saved.getId()));
        event.setObjectType("Asset");
        auditEventPublisher.publishEvent(event);

        return mapToResponse(saved);
    }
}
