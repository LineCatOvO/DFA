# Task-023: 构建QEMU+Docker镜像

**创建时间**：2026-03-15
**更新时间**：2026-03-16
**优先级**：P0（MVP核心）
**状态**：待处理
**MVP阶段**：阶段1 - 基础设施准备
**完成时间**：

## 任务描述
构建预装Docker Engine的QEMU镜像，支持开箱即用的Docker容器运行。

## 背景
架构重构后，DFA项目使用Termux+QEMU运行虚拟机。需要构建预装Docker的QEMU镜像。

## 架构变更说明
- **原方案**：基于Microdroid或Debian NoCloud
- **新方案**：基于Termux+QEMU架构

## 执行计划
- [ ] 设计镜像构建方案（基于Debian Cloud镜像）
- [ ] 创建镜像构建脚本
- [ ] 安装Docker Engine和containerd
- [ ] 配置SSH服务自动启动
- [ ] 配置Docker服务自动启动
- [ ] 创建cloud-init配置
- [ ] 测试镜像功能

## 知识点记录
### 技术要点
- 基础镜像：Debian 12/13 Cloud镜像（QCOW2格式）
- 预装软件：docker-ce、containerd、openssh-server
- QEMU网络：用户模式网络（user mode networking）
- 端口转发：hostfwd参数配置
- cloud-init：初始化配置

### 注意事项
- 镜像大小控制在500MB以内
- 确保SSH服务自动启动
- 确保Docker服务自动启动
- 配置正确的存储驱动（overlay2）
- 处理权限和安全问题

## 验收标准
- [ ] 镜像成功构建（QCOW2格式）
- [ ] 镜像大小<500MB
- [ ] QEMU可启动镜像
- [ ] SSH服务可连接（端口2222或自定义）
- [ ] Docker服务可访问
- [ ] 可以运行hello-world容器

## 相关资源
- [Debian Cloud Images](https://cloud.debian.org/images/cloud/)
- [Docker安装文档](https://docs.docker.com/engine/install/debian/)
- [QEMU用户模式网络](https://wiki.qemu.org/Documentation/Networking#User_Networking)
- [QemuConfig.kt](../core/vm/src/main/java/com/dfa/core/vm/qemu/QemuConfig.kt)