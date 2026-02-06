package com.asset.assets.controller;

import com.asset.assets.model.Asset;
import com.asset.assets.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    @Autowired
    private AssetRepository assetRepository;

    // Public endpoints для проверки
    @GetMapping("/test")
    public String test() {
        return "Asset Service with PostgreSQL and Security WORKING! Time: " + System.currentTimeMillis();
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("""
            {
                "status": "UP",
                "service": "asset-service",
                "database": "PostgreSQL",
                "security": "enabled"
            }
            """);
    }


    @GetMapping("/db-check")
    public ResponseEntity<?> dbCheck() {
        try {
            long count = assetRepository.count();
            return ResponseEntity.ok().body("""
                {
                    "message": "PostgreSQL connected successfully",
                    "assetCount": %d,
                    "url": "jdbc:postgresql://postgres-assets:5432/assets"
                }
                """.formatted(count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("""
                {
                    "error": "Database connection failed",
                    "message": "%s"
                }
                """.formatted(e.getMessage()));
        }
    }

    // Protected endpoints (требуют аутентификацию)
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<Asset> getAsset(@PathVariable Long id) {
        return assetRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Asset createAsset(@RequestBody Asset asset,
                             @AuthenticationPrincipal Jwt jwt) {
        // В тестовом режиме
        String username = (jwt != null) ?
                jwt.getClaimAsString("preferred_username") : "system";
        asset.setOwner(username);
        return assetRepository.save(asset);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public List<Asset> getMyAssets(@AuthenticationPrincipal Jwt jwt) {
        String username = (jwt != null) ?
                jwt.getClaimAsString("preferred_username") : "system";
        return assetRepository.findByOwner(username);
    }
}