package com.example.assetservice.repository;

import com.example.assetservice.model.entity.Threat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThreatRepository extends JpaRepository<Threat, Long> {
    Page<Threat> findByNameContainingIgnoreCase(String name, Pageable pageable);
}