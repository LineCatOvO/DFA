# 常见问题解答 (FAQ)

本文档收集了 DFA 使用过程中的常见问题及其解答。

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

### Q1: 什么是 DFA？

**DFA (Docker For Android)** 是一个开源项目，利用 Android 的 AVF（Android Virtualization Framework）框架，在 Android 设备上实现原生 Docker 容器支持。

### Q2: 什么是 AVF？

**AVF (Android Virtualization Framework)** 是 Android 13 引入的虚拟化框架，允许在 Android 设备上安全地运行虚拟机。它提供了硬件级别的隔离，确保虚拟机与宿主系统的安全性。

### Q3: DFA 与 Termux 中的 Docker 有什么区别？

| 特性 | DFA | Termux Docker |
|------|-----|---------------|
| 虚拟化方式 | AVF 虚拟机 | 容器/模拟 |
| 性能 | 原生性能 | 模拟性能较低 |
| 隔离性 | 硬件级隔离 | 软件隔离 |
| 兼容性 | 完整 Docker | 部分功能受限 |
| 系统要求 | Android 13+ | Android 7+ |

### Q4: 什么是 Protected VM？

**Protected VM (pVM)** 是 AVF 提供的一种安全虚拟机，具有以下特性：
- 独立的加密内存空间
- 与宿主系统完全隔离
- 无法被宿主系统访问或修改
- 适合运行敏感工作负载

### Q5: DFA 支持哪些 Docker 功能？

DFA 支持大部分标准 Docker 功能：

| 功能 | 支持状态 |
|------|----------|
| 镜像管理 | ✅ 完全支持 |
| 容器生命周期 | ✅ 完全支持 |
| 网络配置 | ✅ 完全支持 |
| 数据卷 | ✅ 完全支持 |
| Docker Compose | ✅ 支持 |
| 多架构镜像 | ⚠️ 部分支持 |
| GPU 直通 | ❌ 不支持 |

---

## 安装问题

### Q6: 我的设备支持 DFA 吗？

检查设备兼容性：

```bash
# 检查 Android 版本（需要 >= 13）
adb shell getprop ro.build.version.sdk

# 检查 CPU 架构（需要 arm64-v8a）
adb shell getprop ro.product.cpu.abi

# 检查 KVM 支持
adb shell ls /dev/kvm
```

### Q7: 为什么需要 Android 13+？

DFA 依赖 AVF 框架，该框架从 Android 13 (API 33) 开始提供。早期 Android 版本不支持 AVF。

### Q8: 安装时提示"INSTALL_FAILED_NO_MATCHING_ABIS"怎么办？

这个错误表示设备 CPU 架构不支持。DFA 目前仅支持 ARM64 (aarch64) 架构。

```bash
# 检查设备架构
adb shell getprop ro.product.cpu.abi
# 应输出 arm64-v8a
```

### Q9: 安装后无法启动怎么办？

1. 检查 KVM 支持：
```bash
adb shell ls -la /dev/kvm
```

2. 检查权限：
```bash
adb shell pm list permissions -g | grep -i virtualization
```

3. 查看崩溃日志：
```bash
adb logcat -s AndroidRuntime:E | grep -A 20 "com.dfa.app"
```

---

## 使用问题

### Q10: 如何运行第一个容器？

```bash
# 运行 Hello World
dfa docker run hello-world

# 运行 Nginx 服务
dfa docker run -d -p 8080:80 --name my-nginx nginx

# 访问服务
# 在浏览器打开 http://localhost:8080
```

### Q11: 如何访问容器内的服务？

DFA 支持端口映射，将容器端口映射到 Android 设备：

```bash
# 映射单个端口
dfa docker run -d -p 8080:80 nginx

# 映射多个端口
dfa docker run -d -p 8080:80 -p 8443:443 nginx

# 查看端口映射
dfa docker port <container-id>
```

### Q12: 如何持久化容器数据？

使用数据卷持久化数据：

```bash
# 创建数据卷
dfa docker volume create mydata

# 使用数据卷
dfa docker run -d -v mydata:/data nginx

# 挂载本地目录
dfa docker run -d -v /sdcard/DFA/data:/data nginx
```

### Q13: 如何使用 Docker Compose？

```bash
# 安装 Docker Compose
dfa plugin install docker-compose

# 使用 Compose 文件
dfa docker-compose up -d

# 查看服务状态
dfa docker-compose ps
```

### Q14: 如何进入容器终端？

```bash
# 进入运行中的容器
dfa docker exec -it <container-id> /bin/sh

# 或使用 bash
dfa docker exec -it <container-id> /bin/bash
```

### Q15: 如何查看容器日志？

```bash
# 查看日志
dfa docker logs <container-id>

# 实时查看日志
dfa docker logs -f <container-id>

# 查看最近 100 行
dfa docker logs --tail 100 <container-id>
```

---

## 技术问题

### Q16: Docker 命令响应很慢怎么办？

可能原因和解决方案：

1. **资源不足**：增加 VM 资源配置
```yaml
vm:
  memory: 4096
  cpus: 4
```

2. **存储性能**：使用更快的存储
```bash
# 检查存储性能
dfa storage benchmark
```

3. **网络延迟**：使用本地镜像仓库
```bash
# 配置镜像加速
dfa config set registry.mirror https://mirror.example.com
```

### Q17: 容器无法访问网络怎么办？

```bash
# 检查网络配置
dfa docker network ls

# 测试网络连接
dfa docker run --rm alpine ping -c 3 8.8.8.8

# 检查 DNS
dfa docker run --rm alpine nslookup google.com

# 使用自定义 DNS
dfa docker run --dns 8.8.8.8 <image>
```

### Q18: 如何调试容器问题？

```bash
# 查看容器详情
dfa docker inspect <container-id>

# 查看容器进程
dfa docker top <container-id>

# 查看资源使用
dfa docker stats <container-id>

# 导出容器日志
dfa docker logs <container-id> > container.log
```

### Q19: 如何更新 DFA？

```bash
# 检查更新
dfa update check

# 更新到最新版本
dfa update install

# 或手动更新
adb install -r dfa-new.apk
```

### Q20: 如何备份和恢复？

```bash
# 备份配置
dfa config export > dfa-config.yaml

# 备份数据卷
dfa volume backup mydata > mydata.tar.gz

# 恢复配置
dfa config import < dfa-config.yaml

# 恢复数据卷
dfa volume restore mydata < mydata.tar.gz
```

---

## 性能问题

### Q21: DFA 的性能如何？

性能取决于设备硬件和配置：

| 配置 | 容器启动时间 | 网络吞吐量 |
|------|--------------|------------|
| 低配 (2GB/1CPU) | 5-10秒 | ~100Mbps |
| 中配 (4GB/2CPU) | 2-5秒 | ~500Mbps |
| 高配 (8GB/4CPU) | 1-2秒 | ~1Gbps |

### Q22: 如何优化 DFA 性能？

1. **增加资源**：
```yaml
vm:
  memory: 4096
  cpus: 4
  storage: 20
```

2. **使用镜像缓存**：
```bash
# 预拉取常用镜像
dfa docker pull nginx:alpine
dfa docker pull redis:alpine
```

3. **优化存储**：
```bash
# 清理未使用资源
dfa docker system prune -a
```

### Q23: 内存不足怎么办？

```bash
# 检查内存使用
dfa stats memory

# 降低 VM 内存
dfa config set vm.memory 1024

# 限制容器内存
dfa docker run --memory="256m" <image>
```

### Q24: 存储空间不足怎么办？

```bash
# 检查存储使用
dfa docker system df

# 清理未使用资源
dfa docker system prune -a

# 清理镜像缓存
dfa docker image prune -a

# 清理数据卷
dfa docker volume prune
```

---

## 安全相关

### Q25: DFA 安全吗？

DFA 采用多层安全机制：

1. **AVF 隔离**：虚拟机与宿主系统隔离
2. **Protected VM**：硬件级别的安全隔离
3. **容器安全**：Docker 原生安全特性
4. **权限控制**：细粒度的权限管理

### Q26: 如何确保容器安全？

```bash
# 以非 root 用户运行
dfa docker run --user 1000:1000 <image>

# 限制容器能力
dfa docker run --cap-drop ALL <image>

# 只读根文件系统
dfa docker run --read-only <image>

# 使用安全配置
dfa docker run --security-opt no-new-privileges <image>
```

### Q27: 如何管理敏感数据？

```bash
# 使用 Docker Secrets
dfa docker secret create my_secret secret.txt

# 使用 Config
dfa docker config create my_config config.yaml

# 使用环境变量（不推荐敏感数据）
dfa docker run -e API_KEY=xxx <image>
```

### Q28: 如何审计容器活动？

```bash
# 启用审计日志
dfa config set audit.enabled true

# 查看审计日志
dfa audit logs

# 导出审计报告
dfa audit export > audit-report.json
```

---

## 相关文档

- [安装指南](INSTALLATION.md)
- [故障排除](TROUBLESHOOTING.md)
- [安全指南](SECURITY.md)
- [性能指南](PERFORMANCE.md)