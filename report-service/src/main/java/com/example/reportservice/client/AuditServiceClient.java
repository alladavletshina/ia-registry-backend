package com.example.reportservice.client;

import com.example.reportservice.dto.AuditEventDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "audit-service", url = "${services.audit-service.url}")
public interface AuditServiceClient {

    // Для отчёта по безопасности
    @GetMapping("/report")
    List<AuditEventDTO> getReportData(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    );
}