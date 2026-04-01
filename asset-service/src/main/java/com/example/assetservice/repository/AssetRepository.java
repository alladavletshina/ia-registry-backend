package com.example.assetservice.repository;
import com.example.assetservice.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findAllByOwnerId(String ownerId);

    Asset getAssetById(Long id);
}
