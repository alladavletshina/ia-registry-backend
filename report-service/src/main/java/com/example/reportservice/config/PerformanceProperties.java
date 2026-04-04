package com.example.reportservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "performance")
public class PerformanceProperties {
    private List<ServiceConfig> services;

    @Data
    public static class ServiceConfig {
        private String name;
        private String url;
    }
}
