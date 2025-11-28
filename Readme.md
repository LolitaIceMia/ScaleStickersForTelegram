# 🚀 FoxSoftware Telegram Sticker Scaler

**高性能 Telegram 贴纸批量处理引擎**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://jdk.java.net/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.2-green.svg)](https://spring.io/projects/spring-boot)

## 📖 简介 (Introduction)

本项目是一个专为高性能主机（如 i9-14900K + 64GB RAM）设计的图像批处理工具。它利用 **Java 21 虚拟线程 (Virtual Threads)** 技术，能够以极高的并发度将任意尺寸的 PNG 图片转换为 Telegram 贴纸标准格式（512x512, 透明背景）。

## ✨ 核心特性 (Features)

* **⚡ 极致性能**：基于 Java 21 Project Loom (虚拟线程) 构建，结合 Semaphore 流量控制，完美压榨多核 CPU 性能。
* **🎨 工业级画质**：强制启用 **双三次插值 (Bicubic)**、抗锯齿 (Anti-aliasing) 和高质量渲染模式，拒绝马赛克。
* **📐 智能缩放**：
    * 保持原图宽高比（不拉伸变形）。
    * 自动居中绘制。
    * 不足部分自动填充透明背景 (Alpha Channel)。
* **🛡️ 安全可靠**：内置路径遍历攻击 (Path Traversal) 检测，防止恶意文件操作。
* **⚙️ 灵活配置**：通过 YAML 配置文件自定义输入/输出路径及并发度。

## 🛠️ 技术栈 (Tech Stack)

* **Language**: Java 21 (Corretto / OpenJDK)
* **Framework**: Spring Boot 3.5.2
* **Concurrency**: Virtual Threads + Semaphore
* **Image Processing**: Java AWT / ImageIO (High Quality Rendering Hints)

## 🚀 快速开始 (Getting Started)

### 1. 环境要求
* JDK 21 或更高版本。
* Maven 3.8+。

### 2. 配置应用
修改 `src/main/resources/application.yml` 文件以适配您的环境：

```yaml
fox:
  sticker:
    # 输入图片目录 (支持扫描子文件夹)
    input-path: "C:/TelegramStickers/Input"
    # 输出结果目录 (自动创建)
    output-path: "C:/TelegramStickers/Output"
    # 目标尺寸 (Telegram 标准为 512)
    width: 512
    height: 512
    # 并发度 (建议设置为 CPU 逻辑核心数 * 4，视内存大小而定)
    # 对于 i9-14900K + 64GB RAM，推荐 128
    parallelism: 128
````

### 3\. 编译与运行

**使用 Maven 运行：**

```bash
mvn spring-boot:run
```

**或者打包为 JAR 运行：**

```bash
mvn clean package
java -jar target/scale-stickers-for-telegram-1.0.0-SNAPSHOT.jar
```

## 📊 性能表现 (Performance)

> 测试环境：i9-14900K | 64GB DDR5 | RTX 4080 Super | NVMe SSD

在 Full Power 模式下（Parallelism: 128），处理 1000 张混合分辨率素材：

* **平均耗时**：约 2-5 秒 (取决于源文件大小)
* **CPU 占用**：全核满载
* **内存占用**：受控稳定，无 OOM 风险

## 📝 处理逻辑示意

```text
[原始图片 459x384] 
      ⬇️
(计算比例 & 居中坐标)
      ⬇️
[新建 512x512 透明画布]
      ⬇️
(双三次插值缩放绘制)
      ⬇️
[输出 512x512 标准 PNG]
```

## ⚠️ 注意事项

* 仅支持 PNG 格式图片的处理。
* 请确保输入路径中有图片文件，否则程序会提示警告并退出。
* 虽然程序做了内存保护，但若处理超大分辨率（如 8K+）图片，请适当降低 `parallelism` 参数。

-----

© 2025 FoxSoftware. All Rights Reserved.

```
```