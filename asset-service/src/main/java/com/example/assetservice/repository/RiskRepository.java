package com.example.assetservice.repository;

import com.example.assetservice.model.Asset;
import com.example.assetservice.model.entity.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskRepository extends JpaRepository<Risk, UUID> {
    List<Risk> findByAssetOrderByCalculationDateDesc(Asset asset);
}
