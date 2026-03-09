package com.example.reportservice.client;

import com.example.reportservice.dto.TaskStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "task-service", url = "${services.task-service.url}")
public interface TaskServiceClient {
    @GetMapping("/stats")
    TaskStatsDTO getTaskStats();
}