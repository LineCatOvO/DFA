# DFA - Docker For Android

一个用于在Android设备上管理Docker容器的应用程序。

## 项目概述

DFA是一个Android应用，允许用户在移动设备上管理和监控Docker容器。通过集成虚拟机技术和Docker API，提供完整的容器管理功能。

## 技术栈

- **开发语言**: Kotlin
- **最低SDK**: API 33 (Android 13)
- **目标SDK**: API 34 (Android 14)
- **UI框架**: Jetpack Compose
- **构建工具**: Gradle 8.x + KTS
- **依赖注入**: Hilt
- **架构模式**: MVVM + Clean Architecture

## 项目结构

```
DFA/
├── app/                          # 主应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dfa/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── DfaApplication.kt
│   │   │   │   └── ui/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── core/                         # 核心模块
│   ├── vm/                       # 虚拟机管理
│   ├── docker/                   # Docker集成
│   └── common/                   # 公共组件
├── build.gradle.kts              # 根构建文件
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle配置
└── gradle/                       # Gradle wrapper
    └── libs.versions.toml        # 版本目录
```

## 开发环境搭建

### 前置要求

- **JDK**: 17 或更高版本
- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **Android SDK**: API 34
- **Gradle**: 8.x (通过Gradle Wrapper自动管理)

### 环境配置步骤

1. **安装JDK 17**
   ```bash
   # macOS (使用Homebrew)
   brew install openjdk@17
   
   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk
   
   # 验证安装
   java -version
   ```

2. **安装Android Studio**
   - 从 [Android Studio官网](https://developer.android.com/studio) 下载最新版本
   - 安装后打开Android Studio，完成初始设置
   - 在SDK Manager中安装SDK 34

3. **配置环境变量**
   ```bash
   # 添加到 ~/.bashrc 或 ~/.zshrc
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/emulator
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   ```

4. **克隆项目**
   ```bash
   git clone <repository-url>
   cd DFA
   ```

5. **同步项目**
   - 打开Android Studio
   - 选择 "Open an Existing Project"
   - 选择DFA项目目录
   - 等待Gradle同步完成

### 构建项目

```bash
# 调试版本
./gradlew assembleDebug

# 发布版本
./gradlew assembleRelease

# 运行测试
./gradlew test

# 代码检查
./gradlew detekt
```

## 主要依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Jetpack Compose | BOM 2024.02.00 | UI框架 |
| Hilt | 2.50 | 依赖注入 |
| Coroutines | 1.7.3 | 异步处理 |
| Room | 2.6.1 | 本地数据库 |
| Retrofit | 2.9.0 | 网络请求 |
| Navigation Compose | 2.7.7 | 导航组件 |

## 代码规范

项目使用以下工具确保代码质量：

- **Detekt**: Kotlin静态代码分析
- **EditorConfig**: 编辑器配置统一

运行代码检查：
```bash
./gradlew detekt
```

## 模块说明

### app模块
主应用模块，包含UI层和应用入口。

### core:vm模块
虚拟机管理模块，负责虚拟机的创建、配置和生命周期管理。

### core:docker模块
Docker集成模块，提供Docker API的封装和容器操作功能。

### core:common模块
公共组件模块，包含工具类、扩展函数和通用组件。

## 许可证

[待定]

## 贡献指南

[待定]