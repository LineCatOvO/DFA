# Task-023: 构建含Docker的VM镜像

**创建时间**：2026-03-15
**优先级**：P0（MVP核心）
**状态**：待处理
**MVP阶段**：阶段2 - Docker集成

## 任务描述
构建预装Docker Engine的VM镜像，支持开箱即用的Docker容器运行。

## 背景
当前使用标准Debian NoCloud镜像，没有预装Docker Engine，无法直接运行容器。

## 执行计划
- [ ] 设计镜像构建方案
- [ ] 创建Dockerfile（基于Microdroid或Debian）
- [ ] 安装Docker Engine和containerd
- [ ] 配置Docker守护进程
- [ ] 创建启动脚本
- [ ] 测试镜像功能

## 知识点记录
### 技术要点
- 基于Microdroid或Debian NoCloud
- 预装docker-ce、containerd
- 配置VirtIO设备支持
- 优化镜像大小
- QCOW2镜像格式

### 注意事项
- 镜像大小控制在500MB以内
- 确保Docker服务自动启动
- 配置正确的存储驱动
- 处理权限和安全问题

## 验收标准
- [ ] 镜像成功构建
- [ ] Docker Engine可以启动
- [ ] 可以运行hello-world容器
- [ ] 镜像大小<500MB
- [ ] 启动时间<30秒

## 相关资源
- [Microdroid文档](https://android.googlesource.com/platform/packages/modules/Virtualization/+/refs/heads/master/microdroid/)
- [Docker安装文档](https://docs.docker.com/engine/install/debian/)
- [现有ImageConstants.kt](../core/vm/src/main/java/com/dfa/core/vm/image/ImageConstants.kt)