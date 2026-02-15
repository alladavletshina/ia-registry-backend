package com.example.assetservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssetStatus {

    ACTIVE("Активный"),
    NEEDS_REVIEW("Требует проверка"),
    ARCHIVED("Архивирован"),
    DRAFT("Черновик");

    private final String description;
}
