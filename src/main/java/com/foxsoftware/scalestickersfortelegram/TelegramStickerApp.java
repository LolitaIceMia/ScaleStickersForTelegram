package com.foxsoftware.scalestickersfortelegram;

import com.foxsoftware.scalestickersfortelegram.config.StickerConfig;
import com.foxsoftware.scalestickersfortelegram.service.ImageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(StickerConfig.class)
@RequiredArgsConstructor
public class TelegramStickerApp implements CommandLineRunner {

    private final StickerConfig config;
    private final ImageProcessor imageProcessor;

    public static void main(String[] args) {
        SpringApplication.run(TelegramStickerApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Process Start To Running...");

        Path inputDir = Paths.get(config.inputPath());
        Path outputDir = Paths.get(config.outputPath());

        validatePaths(inputDir, outputDir);

        // 扫描所有 PNG 图片
        log.info("正在扫描目录: {}", inputDir);
        List<Path> files;
        try (Stream<Path> stream = Files.walk(inputDir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .toList();
        }

        if (files.isEmpty()) {
            log.warn("未找到任何 PNG 文件！");
            return;
        }

        log.info("📦 扫描到 {} 个文件，准备启动引擎...", files.size());
        Instant start = Instant.now();

        // === 核心并发逻辑 (Java 21 Virtual Threads) ===
        // 虚拟线程虽然廉价，但我们使用 Semaphore 来限制“同时正在处理”的任务数
        // 避免瞬间创建数万个 BufferedImage 对象导致内存波动过大 (虽然你有 64G，但我们要优雅)
        Semaphore semaphore = new Semaphore(config.parallelism());

        // 使用 Java 21 的虚拟线程池
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Path filePath : files) {
                executor.submit(() -> {
                    try {
                        semaphore.acquire(); // 获取令牌
                        imageProcessor.process(filePath, outputDir);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release(); // 释放令牌
                    }
                });
            }
        } // try-with-resources 会自动等待所有虚拟线程执行完毕 (close 方法会 block 直到结束)

        Instant end = Instant.now();
        long millis = Duration.between(start, end).toMillis();
        double seconds = millis / 1000.0;

        log.info("==========================================");
        log.info("🎉 全部完成！");
        log.info("⏱️ 耗时: {} 秒", String.format("%.2f", seconds));
        log.info("🚀 平均速度: {} 张/秒", String.format("%.2f", files.size() / seconds));
        log.info("==========================================");
    }

    private void validatePaths(Path input, Path output) throws IOException {
        if (!Files.exists(input)) {
            throw new IOException("错误：输入目录不存在 -> " + input);
        }
        if (!Files.exists(output)) {
            Files.createDirectories(output);
            log.info("已自动创建输出目录 -> {}", output);
        }
    }
}