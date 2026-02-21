package com.example.assetservice.service;

import com.example.assetservice.dto.AssetResponse;
import com.example.assetservice.model.Asset;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.repository.AssetRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    @Transactional
    public Asset createAsset(CreateAssetRequest request) {
        Asset asset = new Asset();
        asset.setName(request.getName());
        asset.setCategory(request.getCategory());
        asset.setOwnerId(request.getOwnerId());
        asset.setStatus(request.getStatus());
        asset.setConfidentiality(request.getConfidentiality());
        asset.setIntegrity(request.getIntegrity());
        asset.setAvailability(request.getAvailability());
        asset.setLastReview(request.getLastReview());
        asset.setDescription(request.getDescription());
        asset.setLocation(request.getLocation());
        asset.setTags(request.getTags());

        return assetRepository.save(asset);
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
        response.setCategory(asset.getCategory());
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
        return response;
    }

    public AssetResponse getAssetById(long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Asset not found with id: " + id));
        return mapToResponse(asset);
    }

    @Transactional
    public AssetResponse updateAsset(long id, @Valid CreateAssetRequest request) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Актив с id" + id + " не найден"));

        asset.setName(request.getName());
        asset.setCategory(request.getCategory());
        asset.setOwnerId(request.getOwnerId());
        asset.setStatus(request.getStatus());
        asset.setConfidentiality(request.getConfidentiality());
        asset.setIntegrity(request.getIntegrity());
        asset.setAvailability(request.getAvailability());
        asset.setLastReview(request.getLastReview());
        asset.setDescription(request.getDescription());
        asset.setLocation(request.getLocation());
        asset.setTags(request.getTags());
        asset.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(asset);
    }

    @Transactional
    public void deleteAsset(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Актив с id " + id + " не найден");
        }
        assetRepository.deleteById(id);
    }
}
