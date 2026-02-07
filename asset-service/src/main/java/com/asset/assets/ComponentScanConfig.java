package com.asset.assets;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.asset.assets")
public class ComponentScanConfig {
    // Этот класс заставляет Spring сканировать все пакеты
}
