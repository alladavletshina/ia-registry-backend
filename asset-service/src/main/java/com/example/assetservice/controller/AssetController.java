package com.example.assetservice.controller;

import com.example.assetservice.dto.AssetResponse;
import com.example.assetservice.model.Asset;
import com.example.assetservice.dto.CreateAssetRequest;
import com.example.assetservice.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Активы", description = "Управление информационными активами")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать новый актив", description = "Только для администраторов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Актив успешно создан",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public ResponseEntity<Asset> createAsset(@Valid @RequestBody CreateAssetRequest request) {

        Asset created = assetService.createAsset(request);
        return new ResponseEntity<>(created,HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получить список всех активов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список активов",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован")
    })
    public ResponseEntity<List<AssetResponse>> getAllAssets(){

        List<AssetResponse> assets = assetService.getAllAssets();
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получить актив по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Актив найден",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "404", description = "Актив не найден"),
            @ApiResponse(responseCode = "401", description = "Неавторизован")
    })
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable long id) {

        AssetResponse asset = assetService.getAssetById(id);
        return ResponseEntity.ok(asset);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить актив", description = "Полное обновление всех полей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Актив обновлён",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные"),
            @ApiResponse(responseCode = "404", description = "Актив не найден"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable long id,
            @Valid @RequestBody CreateAssetRequest request) {
        AssetResponse updated = assetService.updateAsset(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить актив", description = "Только для администраторов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Актив успешно удалён"),
            @ApiResponse(responseCode = "404", description = "Актив не найден"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public ResponseEntity<Void> deleteAsset(@PathVariable long id){

        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}
