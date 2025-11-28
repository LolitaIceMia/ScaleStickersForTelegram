package com.foxsoftware.scalestickersfortelegram.service;

import com.foxsoftware.scalestickersfortelegram.config.StickerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessor {

    private final StickerConfig config;

    /**
     * 处理单张图片的核心逻辑
     */
    public void process(Path sourcePath, Path outputDir) {
        File sourceFile = sourcePath.toFile();
        String fileName = sourceFile.getName();

        try {
            // 1. 安全检查：防止路径穿越
            if (!sourcePath.normalize().toString().startsWith(Path.of(config.inputPath()).normalize().toString())) {
                throw new SecurityException("非法路径访问阻断: " + sourcePath);
            }

            // 2. 读取图片
            BufferedImage original = ImageIO.read(sourceFile);
            if (original == null) {
                log.warn("跳过无效文件 (无法解码): {}", fileName);
                return;
            }

            // 3. 执行高质量缩放
            BufferedImage processed = resizeAndPad(original);

            // 4. 写入输出文件
            File destFile = outputDir.resolve(fileName).toFile();
            ImageIO.write(processed, "png", destFile);

            // 既然是高性能主机，日志可以只在 Debug 模式开，或者每隔一定数量打印，避免 IO 拖慢 CPU
            // 这里为了爽，我们保留日志，但使用了 logback 异步输出（Spring Boot 默认优化）
            log.info("✅ 处理完成: {}", fileName);

        } catch (Exception e) {
            log.error("❌ 处理失败 [{}]: {}", fileName, e.getMessage());
        }
    }

    /**
     * 图像算法：等比缩放 + 居中 + 透明填充
     */
    private BufferedImage resizeAndPad(BufferedImage original) {
        int targetW = config.width();
        int targetH = config.height();
        int type = BufferedImage.TYPE_INT_ARGB; // 关键：支持透明通道

        BufferedImage targetImage = new BufferedImage(targetW, targetH, type);
        Graphics2D g2 = targetImage.createGraphics();

        try {
            // === 开启 14900K 级别的画质设定 ===
            // 抗锯齿
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 双三次插值 (Bicubic) - 缩放质量之王
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // 渲染质量优先
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // 文本抗锯齿 (防止贴图里有文字锯齿)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 计算缩放逻辑
            double originalW = original.getWidth();
            double originalH = original.getHeight();
            double scale = Math.min((double) targetW / originalW, (double) targetH / originalH);

            int drawW = (int) (originalW * scale);
            int drawH = (int) (originalH * scale);

            // 计算居中坐标
            int x = (targetW - drawW) / 2;
            int y = (targetH - drawH) / 2;

            // 绘制
            g2.drawImage(original, x, y, drawW, drawH, null);

        } finally {
            g2.dispose();
        }

        return targetImage;
    }
}