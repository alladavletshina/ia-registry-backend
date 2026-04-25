package com.example.assetservice.service.fstec;

import com.example.assetservice.config.FstecConfig;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FstecSyncService {

    private final ThreatRepository threatRepository;
    private final FstecConfig fstecConfig;
    private final XlsxThreatParser xlsxThreatParser;
    private final OdsThreatParser odsThreatParser;
    private final RestTemplate restTemplate;

    /**
     * Основной метод синхронизации – только попытка скачать с URL.
     * При неудаче логирует и завершается (без исключения).
     */
    @Retryable(maxAttempts = 1, backoff = @Backoff(delay = 5000, multiplier = 2))
    @Transactional
    public boolean syncThreats() {
        log.info("Запуск синхронизации угроз");
        try (InputStream inputStream = getThreatInputStream()) {
            if (inputStream == null) {
                log.error("Не удалось получить файл угроз, синхронизация прервана");
                return false;
            }
            syncFromInputStream(inputStream);
            return true;
        } catch (Exception e) {
            log.error("Ошибка синхронизации: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Попытка скачать файл по URL.
     * @return InputStream или null, если скачать не удалось
     */
    private InputStream getThreatInputStream() throws IOException {
        if (fstecConfig.getThreatUrl() == null || fstecConfig.getThreatUrl().isEmpty()) {
            log.warn("URL для скачивания угроз не задан в конфигурации");
            return null;
        }
        try {
            log.info("Попытка скачать файл угроз с URL: {}", fstecConfig.getThreatUrl());
            byte[] fileBytes = downloadFile(fstecConfig.getThreatUrl());
            log.info("Файл успешно скачан, размер {} байт", fileBytes.length);
            return new ByteArrayInputStream(fileBytes);
        } catch (Exception e) {
            log.warn("Не удалось скачать файл по URL: {}", e.getMessage());
            return null;
        }
    }

    private byte[] downloadFile(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "AssetManagementSystem/1.0");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RestClientException("Не удалось скачать файл, HTTP статус: " + response.getStatusCode());
    }

    /**
     * Обработка InputStream (общая логика парсинга и сохранения в БД).
     */
    private void syncFromInputStream(InputStream inputStream) throws Exception {
        FstecThreatParser parser = getParser();
        if (parser == null) {
            throw new IllegalStateException("Не указан или неверный тип парсера: " + fstecConfig.getParserType());
        }
        List<Threat> parsedThreats = parser.parse(inputStream);
        log.info("Парсинг завершён, получено {} угроз", parsedThreats.size());

        int inserted = 0, updated = 0;
        for (Threat parsed : parsedThreats) {
            Threat existing = threatRepository.findById(parsed.getId()).orElse(null);
            if (existing == null) {
                threatRepository.save(parsed);
                inserted++;
            } else {
                updateThreat(existing, parsed);
                threatRepository.save(existing);
                updated++;
            }
        }
        log.info("Синхронизация завершена. Добавлено: {}, обновлено: {}", inserted, updated);
    }

    /**
     * Синхронизация из загруженного администратором файла (multipart).
     * Можно вызвать через REST эндпоинт.
     */
    @Transactional
    public void syncFromUploadedFile(MultipartFile file) {
        log.info("Запущена синхронизация из загруженного файла: {}", file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            syncFromInputStream(inputStream);
        } catch (Exception e) {
            log.error("Ошибка при обработке загруженного файла", e);
            throw new RuntimeException("Не удалось обработать загруженный файл", e);
        }
    }

    private FstecThreatParser getParser() {
        String type = fstecConfig.getParserType();
        if ("xlsx".equalsIgnoreCase(type)) {
            return xlsxThreatParser;
        } else if ("ods".equalsIgnoreCase(type)) {
            return odsThreatParser;
        }
        return null;
    }

    private void updateThreat(Threat existing, Threat parsed) {
        existing.setName(parsed.getName());
        existing.setDescription(parsed.getDescription());
        existing.setSource(parsed.getSource());
        existing.setObjectAffected(parsed.getObjectAffected());
        existing.setConfidentiality(parsed.isConfidentiality());
        existing.setIntegrity(parsed.isIntegrity());
        existing.setAvailability(parsed.isAvailability());
        existing.setInclusionDate(parsed.getInclusionDate());
        existing.setLastModified(parsed.getLastModified());
        existing.setStatus(parsed.getStatus());
        existing.setNotes(parsed.getNotes());
        existing.setSyncedAt(LocalDate.now());
    }
}