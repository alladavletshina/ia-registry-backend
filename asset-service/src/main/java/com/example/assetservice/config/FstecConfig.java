package com.example.assetservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fstec")
public class FstecConfig {
    private String threatUrl;
    private String parserType;
    private String syncCron;
    private int connectionTimeout = 10000;
    private int readTimeout = 30000;
    private String fallbackFilePath = "/thrlist.xlsx";
}