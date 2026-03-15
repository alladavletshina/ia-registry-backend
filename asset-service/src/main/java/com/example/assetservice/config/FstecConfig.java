package com.example.assetservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fstec")
public class FstecConfig {

    /*URL для скачивания файла с угрозами*/
    private String threatUrl;
    /*"xlsx" или "ods"*/
    private String parserType;
    /*cron-выражение для планировщика*/
    private String syncCron;
}