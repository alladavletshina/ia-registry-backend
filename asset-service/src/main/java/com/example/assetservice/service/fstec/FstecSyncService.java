package com.example.assetservice.service.fstec;

import com.example.assetservice.config.FstecConfig;
import com.example.assetservice.model.entity.Threat;
import com.example.assetservice.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FstecSyncService {

    private final ThreatRepository threatRepository;
    private final FstecConfig fstecConfig;
    private final FstecThreatParser xlsxThreatParser;
    private final FstecThreatParser odsThreatParser;

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    @Transactional
    public void syncThreats() {
        log.info("Начало синхронизации угроз из встроенного файла");

        // Читаем файл из classpath (resources)
        try (InputStream is = getClass().getResourceAsStream("/thrlist.xlsx")) {
            if (is == null) {
                log.error("Файл thrlist.xlsx не найден в classpath. Проверьте, что файл находится в src/main/resources.");
                return;
            }

            FstecThreatParser parser = getParser();
            if (parser == null) {
                log.error("Не указан или неверный тип парсера: {}", fstecConfig.getParserType());
                return;
            }

            List<Threat> parsedThreats = parser.parse(is);
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
        } catch (Exception e) {
            log.error("Ошибка при синхронизации: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка синхронизации", e);
        }
    }

    @Recover
    public void recover(Exception e) {
        log.error("Все попытки синхронизации не удались", e);
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