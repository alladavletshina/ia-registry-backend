package com.example.assetservice.service.fstec;

import com.example.assetservice.config.FstecConfig;
import com.example.assetservice.repository.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FstecSyncService syncService;

    @Test
    void syncThreats_whenDownloadFails_shouldReturnFalseAndNotSave() {
        when(fstecConfig.getThreatUrl()).thenReturn("https://example.com/missing.xlsx");

        doThrow(new RuntimeException("Network error"))
                .when(restTemplate).exchange(anyString(), any(), any(), eq(byte[].class));

        boolean result = syncService.syncThreats();

        assertFalse(result);

        verify(threatRepository, never()).save(any());

        verify(fstecConfig, atLeastOnce()).getThreatUrl();
    }
}