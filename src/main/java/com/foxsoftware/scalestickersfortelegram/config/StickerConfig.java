package com.foxsoftware.scalestickersfortelegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fox.sticker")
public record StickerConfig(
        String inputPath,
        String outputPath,
        int width,
        int height,
        int parallelism
) {}