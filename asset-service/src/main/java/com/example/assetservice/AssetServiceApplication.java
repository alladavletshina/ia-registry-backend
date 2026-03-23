package com.example.assetservice;

import com.example.assetservice.util.SSLUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class AssetServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(AssetServiceApplication.class, args);
    }

}
