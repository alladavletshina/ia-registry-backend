package com.example.assetservice.service.fstec;

import com.example.assetservice.config.FstecConfig;
import com.example.assetservice.repository.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FstecSyncServiceTest {

    @Mock
    private ThreatRepository threatRepository;

    @Mock
    private FstecConfig fstecConfig;

    @Mock
    private FstecThreatParser xlsxThreatParser;

    @Mock
    private FstecThreatParser odsThreatParser;

    @InjectMocks
    private FstecSyncService syncService;

    @Test
    void syncThreats_whenFileNotFound_shouldLogAndReturn() throws Exception {
        // Файл thrlist.xlsx отсутствует в classpath тестов – это не критично, метод просто вернётся
        when(fstecConfig.getParserType()).thenReturn("xlsx");

        // Не выбрасываем исключение, просто проверяем, что метод не упал
        syncService.syncThreats();

        verify(threatRepository, never()).save(any());
    }
}