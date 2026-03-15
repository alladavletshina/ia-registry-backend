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

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FstecSyncService {

    private final ThreatRepository threatRepository;
    private final FstecConfig fstecConfig;

    // Выбор парсера в зависимости от типа
    private final FstecThreatParser xlsxThreatParser;
    private final FstecThreatParser odsThreatParser;

    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    @Transactional
    public void syncThreats() {
        log.info("Начало синхронизации угроз из БДУ ФСТЭК. URL: {}", fstecConfig.getThreatUrl());

        HttpURLConnection connection = null;
        try {
            URL url = new URL(fstecConfig.getThreatUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("Не удалось скачать файл. HTTP код: {}", responseCode);
                return;
            }

            int contentLength = connection.getContentLength();
            if (contentLength <= 0) {
                log.warn("Размер файла не определён или файл пуст (contentLength={})", contentLength);
            } else {
                log.info("Размер файла: {} байт", contentLength);
            }

            try (InputStream is = connection.getInputStream()) {
                // Проверка на пустой поток
                if (is.available() == 0) {
                    log.error("Входной поток пуст, файл не содержит данных");
                    return;
                }

                // Выбор парсера
                FstecThreatParser parser = getParser();
                if (parser == null) {
                    log.error("Не указан или неверный тип парсера: {}", fstecConfig.getParserType());
                    return;
                }

                List<Threat> parsedThreats = parser.parse(is);
                log.info("Парсинг завершён, получено {} угроз", parsedThreats.size());

                // Сохранение в БД
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

        } catch (Exception e) {
            log.error("Ошибка при синхронизации: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка синхронизации с ФСТЭК", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Recover
    public void recover(IOException e) {
        log.error("Все попытки синхронизации не удались", e);
        // Здесь можно отправить уведомление администратору
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