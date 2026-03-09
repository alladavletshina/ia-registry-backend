package com.example.reportservice.client;

import com.example.reportservice.dto.AssetDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "asset-service", url = "${services.asset-service.url}")
public interface AssetServiceClient {
    @GetMapping
    List<AssetDTO> getAllAssets();
}