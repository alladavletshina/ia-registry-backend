package com.example.assetservice.controller;

import com.example.assetservice.model.Asset;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<Asset> createAsset(@Valid @RequestBody CreateAssetRequest request) {

        Asset created = assetService.createAsset(request);
        return new ResponseEntity<>(created,HttpStatus.CREATED);
    }
}
