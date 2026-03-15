package com.example.assetservice.scheduler;

import com.example.assetservice.config.FstecConfig;
import com.example.assetservice.service.fstec.FstecSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FstecSyncScheduler {

    private final FstecSyncService syncService;
    private final FstecConfig fstecConfig;

    @Scheduled(cron = "${fstec.sync-cron}")
    public void scheduledSync() {
        log.info("Запуск плановой синхронизации с ФСТЭК по расписанию: {}", fstecConfig.getSyncCron());
        syncService.syncThreats();
    }
}