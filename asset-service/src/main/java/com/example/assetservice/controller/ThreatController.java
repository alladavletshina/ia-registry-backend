package com.example.assetservice.controller;

import com.example.assetservice.dto.ThreatDto;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.ThreatRepository;
import com.example.assetservice.service.fstec.FstecSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
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
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
            @Parameter(description = "Параметры пагинации и сортировки") Pageable pageable) {
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
            @PathVariable @Parameter(description = "Идентификатор угрозы (например, '1')") Long id) {
        Threat threat = threatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Угроза не найдена"));
        return ThreatDto.fromEntity(threat);
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncThreats() {
        try {
            boolean success = fstecSyncService.syncThreats();
            if (success) {
                return ResponseEntity.ok("Синхронизация выполнена.");
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Сервис временно недоступен. Попробуйте загрузить файл вручную через кнопку «Загрузить XLSX».");
            }
        } catch (Exception e) {
            log.error("Ошибка синхронизации", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Сервис временно недоступен. Попробуйте загрузить файл вручную.");
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Загрузить и синхронизировать угрозы из файла XLSX/ODS",
            description = "Администратор может вручную загрузить файл угроз (например, свежую выгрузку с bdu.fstec.ru)")
    public ResponseEntity<String> uploadThreatsFile(@RequestParam("file") MultipartFile file) {
        try {
            fstecSyncService.syncFromUploadedFile(file);
            return ResponseEntity.ok("Файл успешно обработан, справочник угроз обновлён");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при обработке файла: " + e.getMessage());
        }
    }
}