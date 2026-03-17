# 常见问题解答 (FAQ)

本文档收集了 CDroid 使用过程中的常见问题及其解答。

---

## 目录

- [概念解释](#概念解释)
- [安装问题](#安装问题)
- [使用问题](#使用问题)
- [技术问题](#技术问题)
- [性能问题](#性能问题)
- [安全相关](#安全相关)

---

## 概念解释

### Q1: 什么是 CDroid？

**CDroid (Container Dashboard)** 是一个跨平台的容器管理应用，支持连接和管理远程或本地的 Docker、Podman 等容器服务。通过统一的用户界面，提供完整的容器管理功能。

### Q2: 什么是 Docker Context？

**Docker Context** 是 Docker 的一个功能，允许用户管理多个 Docker 守护进程（本地和远程）。通过 Context，可以轻松切换不同的 Docker 环境，如本地开发环境、测试环境和生产环境。

### Q3: CDroid 支持哪些容器服务？

CDroid 支持多种容器服务：

| 服务 | 支持状态 | 说明 |
|------|----------|------|
| Docker | ✅ 完全支持 | 本地和远程Docker服务 |
| Podman | ✅ 支持 | 本地和远程Podman服务 |
| Kubernetes | ⚠️ 计划中 | 未来版本支持 |

### Q4: CDroid 与 Docker Desktop 有什么区别？

| 特性 | CDroid | Docker Desktop |
|------|--------|---------------|
| 平台支持 | Android/iOS/Web | Windows/Mac/Linux |
| 远程管理 | ✅ 支持 | ❌ 不支持 |
| 多Context | ✅ 支持 | ✅ 支持 |
| 资源占用 | 低 | 较高 |
| 开源 | ✅ 100%开源 | ❌ 部分开源 |

### Q5: CDroid 支持哪些 Docker 功能？

CDroid 支持大部分标准 Docker 功能：

| 功能 | 支持状态 |
|------|----------|
| 镜像管理 | ✅ 完全支持 |
| 容器生命周期 | ✅ 完全支持 |
| 网络配置 | ✅ 完全支持 |
| 数据卷 | ✅ 完全支持 |
| Docker Compose | ✅ 支持 |
| 多架构镜像 | ✅ 完全支持 |

---

## 安装问题

### Q6: 我的设备支持 CDroid 吗？

检查设备兼容性：

```bash
# 检查 Android 版本（需要 >= 24）
adb shell getprop ro.build.version.sdk

# 检查 CPU 架构
adb shell getprop ro.product.cpu.abi
```

### Q7: 为什么需要 Android 7.0+？

CDroid 依赖 Android 7.0 (API 24) 提供的网络和存储功能。早期 Android 版本不支持必要的 API。

### Q8: 安装时提示"INSTALL_FAILED_NO_MATCHING_ABIS"怎么办？

这个错误表示设备 CPU 架构不支持。CDroid 支持 ARM64 (aarch64) 和 x86_64 架构。

```bash
# 检查设备架构
adb shell getprop ro.product.cpu.abi
# 应输出 arm64-v8a 或 x86_64
```

### Q9: 安装后无法启动怎么办？

1. 检查网络连接：
```bash
adb shell ping -c 3 8.8.8.8
```

2. 检查权限：
```bash
adb shell pm list permissions -g | grep -i network
```

3. 查看崩溃日志：
```bash
adb logcat -s AndroidRuntime:E | grep -A 20 "com.cdroid.app"
```

---

## 使用问题

### Q10: 如何运行第一个容器？

```bash
# 运行 Hello World
cdroid docker run hello-world

# 运行 Nginx 服务
cdroid docker run -d -p 8080:80 --name my-nginx nginx

# 访问服务
# 在浏览器打开 http://localhost:8080
```

### Q11: 如何访问容器内的服务？

CDroid 支持端口映射，将容器端口映射到 Android 设备：

```bash
# 映射单个端口
cdroid docker run -d -p 8080:80 nginx

# 映射多个端口
cdroid docker run -d -p 8080:80 -p 8443:443 nginx

# 查看端口映射
cdroid docker port <container-id>
```

### Q12: 如何持久化容器数据？

使用数据卷持久化数据：

```bash
# 创建数据卷
cdroid docker volume create mydata

# 使用数据卷
cdroid docker run -d -v mydata:/data nginx

# 挂载本地目录
cdroid docker run -d -v /sdcard/CDroid/data:/data nginx
```

### Q13: 如何使用 Docker Compose？

```bash
# 安装 Docker Compose
cdroid plugin install docker-compose

# 使用 Compose 文件
cdroid docker-compose up -d

# 查看服务状态
cdroid docker-compose ps
```

### Q14: 如何进入容器终端？

```bash
# 进入运行中的容器
cdroid docker exec -it <container-id> /bin/sh

# 或使用 bash
cdroid docker exec -it <container-id> /bin/bash
```

### Q15: 如何查看容器日志？

```bash
# 查看日志
cdroid docker logs <container-id>

# 实时查看日志
cdroid docker logs -f <container-id>

# 查看最近 100 行
cdroid docker logs --tail 100 <container-id>
```

### Q16: 如何切换 Context？

```bash
# 列出所有 Context
cdroid context ls

# 切换 Context
cdroid context use remote

# 查看当前 Context
cdroid context current
```

### Q17: 如何配置 Podman？

```bash
# 创建 Podman Context
cdroid context create podman-local \
  --type podman \
  --endpoint unix:///run/podman/podman.sock

# 切换到 Podman Context
cdroid context use podman-local

# 使用 Podman 运行容器
cdroid docker run hello-world
```

---

## 技术问题

### Q18: Docker 命令响应很慢怎么办？

可能原因和解决方案：

1. **网络延迟**：使用本地镜像仓库
```bash
# 配置镜像加速
cdroid config set registry.mirror https://mirror.example.com
```

2. **远程服务延迟**：切换到本地Context
```bash
cdroid context use local
```

3. **连接超时**：增加超时时间
```yaml
# config.yaml
docker:
  timeout: 60
```

### Q19: 容器无法访问网络怎么办？

```bash
# 检查网络配置
cdroid docker network ls

# 测试网络连接
cdroid docker run --rm alpine ping -c 3 8.8.8.8

# 检查 DNS
cdroid docker run --rm alpine nslookup google.com

# 使用自定义 DNS
cdroid docker run --dns 8.8.8.8 <image>
```

### Q20: 如何调试容器问题？

```bash
# 查看容器详情
cdroid docker inspect <container-id>

# 查看容器进程
cdroid docker top <container-id>

# 查看资源使用
cdroid docker stats <container-id>

# 导出容器日志
cdroid docker logs <container-id> > container.log
```

### Q21: 如何更新 CDroid？

```bash
# 检查更新
cdroid update check

# 更新到最新版本
cdroid update install

# 或手动更新
adb install -r cdroid-new.apk
```

### Q22: 如何备份和恢复？

```bash
# 备份配置
cdroid config export > cdroid-config.yaml

# 备份 Context
cdroid context export > contexts.yaml

# 恢复配置
cdroid config import < cdroid-config.yaml

# 恢复 Context
cdroid context import < contexts.yaml
```

### Q23: 连接超时怎么办？

```bash
# 检查网络连接
ping <remote-host>

# 检查端口开放
telnet <remote-host> 2376

# 检查 TLS 配置
cdroid context inspect remote

# 测试连接
cdroid docker ps
```

### Q24: 如何验证连接？

```bash
# 检查 Context 状态
cdroid context ls

# 测试 Docker 连接
cdroid docker info

# 运行测试容器
cdroid docker run --rm hello-world

# 检查网络延迟
cdroid docker run --rm alpine ping -c 5 8.8.8.8
```

---

## 性能问题

### Q25: CDroid 的性能如何？

性能取决于网络连接和容器服务配置：

| 配置 | 容器启动时间 | 网络吞吐量 |
|------|--------------|------------|
| 本地Docker | 1-3秒 | ~1Gbps |
| 远程Docker (LAN) | 2-5秒 | ~500Mbps |
| 远程Docker (WAN) | 5-10秒 | ~100Mbps |

### Q26: 如何优化 CDroid 性能？

1. **使用本地服务**：
```bash
cdroid context use local
```

2. **使用镜像缓存**：
```bash
# 预拉取常用镜像
cdroid docker pull nginx:alpine
cdroid docker pull redis:alpine
```

3. **优化网络**：
```bash
# 使用有线网络而非Wi-Fi
# 配置DNS加速
cdroid config set dns 8.8.8.8,8.8.4.4
```

### Q27: 网络延迟高怎么办？

```bash
# 检查网络延迟
ping <remote-host>

# 使用更近的服务
cdroid context create local-server \
  --docker "tcp://192.168.1.100:2376"

# 启用压缩
cdroid config set compression true
```

### Q28: 存储空间不足怎么办？

```bash
# 检查存储使用
cdroid docker system df

# 清理未使用资源
cdroid docker system prune -a

# 清理镜像缓存
cdroid docker image prune -a

# 清理数据卷
cdroid docker volume prune
```

---

## 安全相关

### Q25: CDroid 安全吗？

CDroid 采用多层安全机制：

1. **Context 隔离**：不同的 Docker Context 相互隔离
2. **TLS 加密**：远程连接使用 TLS 加密传输
3. **容器安全**：Docker 原生安全特性
4. **权限控制**：细粒度的权限管理

### Q26: 如何确保容器安全？

```bash
# 以非 root 用户运行
cdroid docker run --user 1000:1000 <image>

# 限制容器能力
cdroid docker run --cap-drop ALL <image>

# 只读根文件系统
cdroid docker run --read-only <image>

# 使用安全配置
cdroid docker run --security-opt no-new-privileges <image>
```

### Q27: 如何管理敏感数据？

```bash
# 使用 Docker Secrets
cdroid docker secret create my_secret secret.txt

# 使用 Config
cdroid docker config create my_config config.yaml

# 使用环境变量（不推荐敏感数据）
cdroid docker run -e API_KEY=xxx <image>
```

### Q28: 如何审计容器活动？

```bash
# 启用审计日志
cdroid config set audit.enabled true

# 查看审计日志
cdroid audit logs

# 导出审计报告
cdroid audit export > audit-report.json
```

---

## 相关文档

- [安装指南](INSTALLATION.md)
- [故障排除](TROUBLESHOOTING.md)
- [安全指南](SECURITY.md)
- [性能指南](PERFORMANCE.md)