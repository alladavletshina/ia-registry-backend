package com.example.auditservice.service;

import com.example.auditservice.model.dto.AuditEventDto;
import com.example.auditservice.model.dto.AuditStatsDto;
import com.example.auditservice.model.entity.AuditLog;
import com.example.auditservice.model.entity.Severity;
import com.example.auditservice.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void saveAuditLog_shouldPersistLog() {
        AuditEventDto event = new AuditEventDto();
        event.setAction("CREATE");
        event.setSeverity(Severity.INFO);
        event.setUserId(UUID.randomUUID());
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.saveAuditLog(event);

        verify(repository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void getStats_shouldCalculateCorrectCounts() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();
        when(repository.countByDateRange(any(), any())).thenReturn(10L);
        when(repository.countBySeverityAndDateRange(eq(Severity.INFO), any(), any())).thenReturn(4L);
        when(repository.countBySeverityAndDateRange(eq(Severity.WARNING), any(), any())).thenReturn(3L);
        when(repository.countBySeverityAndDateRange(eq(Severity.DANGER), any(), any())).thenReturn(2L);
        when(repository.countBySeverityAndDateRange(eq(Severity.SUCCESS), any(), any())).thenReturn(1L);

        AuditStatsDto stats = auditService.getStats(start, end);

        assertThat(stats.getTotal()).isEqualTo(10);
        assertThat(stats.getInfo()).isEqualTo(4);
        assertThat(stats.getWarning()).isEqualTo(3);
        assertThat(stats.getDanger()).isEqualTo(2);
        assertThat(stats.getSuccess()).isEqualTo(1);
    }

    @Test
    void deleteAuditLog_shouldCallRepository() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);
        doNothing().when(repository).deleteById(id);

        auditService.deleteAuditLog(id);

        verify(repository).deleteById(id);
    }

    @Test
    void getAuditLogById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> auditService.getAuditLogById(id));
    }
}