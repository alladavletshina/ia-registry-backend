package com.example.assetservice.repository;

import com.example.assetservice.model.entity.AssetGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetGroupRepository extends JpaRepository<AssetGroup, UUID> {
    Optional<AssetGroup> findByCode(String code);
}
