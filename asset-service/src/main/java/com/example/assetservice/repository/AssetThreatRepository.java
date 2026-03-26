package com.example.assetservice.repository;

import com.example.assetservice.model.Asset;
import com.example.assetservice.model.entity.AssetThreat;
import com.example.assetservice.model.entity.Threat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetThreatRepository extends JpaRepository<AssetThreat, UUID> {
    List<AssetThreat> findByAssetAndStatus(Asset asset, String status);
    Optional<AssetThreat> findByAssetAndThreat(Asset asset, Threat threat);
}
