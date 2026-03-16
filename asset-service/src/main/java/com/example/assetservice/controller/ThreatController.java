package com.example.assetservice.controller;

import com.example.assetservice.dto.ThreatDto;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.ThreatRepository;
import com.example.assetservice.service.fstec.FstecSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets/threats")
@RequiredArgsConstructor
@Tag(name = "Угрозы", description = "Управление справочником угроз из БДУ ФСТЭК")
public class ThreatController {

    private final ThreatRepository threatRepository;
    private final FstecSyncService fstecSyncService;

    @GetMapping
    @Operation(summary = "Получить список угроз с пагинацией и поиском",
            description = "Возвращает страницу угроз. Можно фильтровать по наименованию через параметр search")
    public Page<ThreatDto> getAllThreats(
            @RequestParam(required = false) @Parameter(description = "Строка для поиска по наименованию угрозы") String search,
            @PageableDefault(size = 20) @Parameter(description = "Параметры пагинации") Pageable pageable) {
        Page<Threat> page;
        if (search != null && !search.isEmpty()) {
            page = threatRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            page = threatRepository.findAll(pageable);
        }
        return page.map(ThreatDto::fromEntity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить угрозу по ID",
            description = "Возвращает детальную информацию об угрозе по её идентификатору (например, '1')")
    public ThreatDto getThreat(
            @PathVariable @Parameter(description = "Идентификатор угрозы (например, '1')") String id) {
        Threat threat = threatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Угроза не найдена"));
        return ThreatDto.fromEntity(threat);
    }

    @PostMapping("/sync")
    @Operation(summary = "Запустить синхронизацию с БДУ ФСТЭК вручную",
            description = "Инициирует процесс скачивания и обновления данных угроз из файла ФСТЭК. Может выполняться несколько секунд.")
    public ResponseEntity<String> syncThreats() {
        try {
            fstecSyncService.syncThreats();
            return ResponseEntity.ok("Синхронизация с ФСТЭК успешно запущена. Проверьте логи для деталей.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при синхронизации: " + e.getMessage());
        }
    }
}