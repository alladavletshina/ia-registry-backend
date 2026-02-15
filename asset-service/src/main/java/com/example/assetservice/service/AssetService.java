package com.example.assetservice.service;

import com.example.assetservice.model.Asset;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.repository.AssetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
