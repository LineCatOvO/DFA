# Gradle Wrapper JAR 文件说明

此目录应包含 `gradle-wrapper.jar` 文件，该文件是 Gradle Wrapper 的核心组件。

## 如何生成

如果您已经安装了 Gradle，可以运行以下命令来生成完整的 Wrapper 文件：

```bash
gradle wrapper --gradle-version=8.6
```

## 替代方案

如果您没有安装 Gradle，可以：

1. **从官方下载**：访问 https://services.gradle.org/distributions/gradle-8.6-bin.zip 下载 Gradle 发行版，然后运行 `gradle wrapper`

2. **从其他项目复制**：从其他使用相同 Gradle 版本的项目中复制 `gradle-wrapper.jar` 文件

3. **使用 SDKMAN**：
   ```bash
   sdk install gradle 8.6
   gradle wrapper
   ```

## 注意事项

- `gradle-wrapper.jar` 是二进制文件，不应手动编辑
- 确保 `gradle-wrapper.properties` 中的版本号与您需要的 Gradle 版本一致
- 首次运行 `./gradlew` 时，Wrapper 会自动下载指定的 Gradle 发行版