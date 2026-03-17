# 变更日志

本项目的所有重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### 新增 (Added)
- 待添加的新功能

### 变更 (Changed)
- 项目从DFA重命名为CDroid
- 项目定位从AVF容器运行时改为容器服务管理面板
- 架构从AVF虚拟机改为Docker Context + 容器服务API

### 修复 (Fixed)
- 待修复的问题

### 移除 (Removed)
- 待移除的功能

---

## [1.0.0] - 2024-01-15

### 新增 (Added)
- 初始版本发布
- 基于 AVF 的 Docker 容器支持
- Android GUI 应用
- 命令行工具 (dfa CLI)
- 容器管理功能
  - 容器创建、启动、停止、删除
  - 镜像拉取、列表、删除
  - 容器日志查看
  - 容器终端访问
- 网络功能
  - Bridge 网络支持
  - 端口映射
  - DNS 配置
- 存储功能
  - 数据卷管理
  - 本地目录挂载
- 安全特性
  - Protected VM 支持
  - 容器资源限制
  - 安全配置选项

### 文档
- README.md - 项目介绍
- ARCHITECTURE.md - 架构文档
- INSTALLATION.md - 安装指南
- DEVELOPMENT.md - 开发指南
- AVF-GUIDE.md - AVF 框架指南
- DOCKER-INTEGRATION.md - Docker 集成指南
- TROUBLESHOOTING.md - 故障排除
- FAQ.md - 常见问题
- API-REFERENCE.md - API 参考
- DEVICE-SUPPORT.md - 设备支持
- PERFORMANCE.md - 性能指南
- SECURITY.md - 安全指南
- ROADMAP.md - 项目路线图

---

## [0.9.0] - 2024-01-01

### 新增 (Added)
- Beta 版本发布
- 基本的 Docker 容器运行支持
- 简单的 GUI 界面
- 基础 CLI 命令

### 修复 (Fixed)
- 修复 VM 启动超时问题
- 修复容器网络连接问题
- 修复内存泄漏问题

---

## [0.8.0] - 2023-12-15

### 新增 (Added)
- Alpha 版本发布
- AVF 虚拟机集成
- Docker 引擎集成
- 基础容器功能

### 已知问题
- VM 启动时间较长
- 部分设备兼容性问题
- 网络功能不完整

---

## 版本说明

### 版本号格式

```
MAJOR.MINOR.PATCH

MAJOR: 不兼容的 API 变更
MINOR: 向后兼容的功能新增
PATCH: 向后兼容的问题修复
```

### 变更类型

| 类型 | 说明 |
|------|------|
| Added | 新增功能 |
| Changed | 现有功能的变更 |
| Deprecated | 即将废弃的功能 |
| Removed | 已移除的功能 |
| Fixed | 问题修复 |
| Security | 安全相关修复 |

---

## 升级指南

### 从 0.9.x 升级到 1.0.0

1. 备份配置和数据
```bash
dfa config export > config-backup.yaml
dfa volume export > volumes-backup.tar
```

2. 卸载旧版本
```bash
adb uninstall com.dfa.app
```

3. 安装新版本
```bash
adb install dfa-1.0.0.apk
```

4. 恢复配置和数据
```bash
dfa config import < config-backup.yaml
dfa volume import < volumes-backup.tar
```

---

## 路线图

详细的版本规划请参阅 [ROADMAP.md](docs/ROADMAP.md)。

---

[Unreleased]: https://github.com/your-org/dfa/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/your-org/dfa/compare/v0.9.0...v1.0.0
[0.9.0]: https://github.com/your-org/dfa/compare/v0.8.0...v0.9.0
[0.8.0]: https://github.com/your-org/dfa/releases/tag/v0.8.0