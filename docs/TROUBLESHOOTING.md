# 故障排除指南

本文档提供 DFA 使用过程中常见问题的诊断和解决方案。

---

## 目录

- [诊断工具](#诊断工具)
- [常见问题](#常见问题)
- [错误代码参考](#错误代码参考)
- [日志分析](#日志分析)
- [解决方案](#解决方案)
- [获取帮助](#获取帮助)

---

## 诊断工具

### DFA 诊断命令

```bash
# 查看系统状态
dfa status

# 查看详细诊断信息
dfa diagnose

# 检查 VM 状态
dfa vm status

# 检查 Docker 状态
dfa docker info

# 查看日志
dfa logs --tail 100
```

### 系统检查脚本

```bash
#!/bin/bash
# dfa-diagnose.sh - DFA 诊断脚本

echo "=== DFA 系统诊断 ==="
echo ""

echo "1. 检查设备兼容性..."
echo "   Android 版本: $(adb shell getprop ro.build.version.release)"
echo "   SDK 版本: $(adb shell getprop ro.build.version.sdk)"
echo "   CPU 架构: $(adb shell getprop ro.product.cpu.abi)"
echo ""

echo "2. 检查 KVM 支持..."
if adb shell ls /dev/kvm &>/dev/null; then
    echo "   ✅ KVM 设备存在"
    adb shell ls -la /dev/kvm
else
    echo "   ❌ KVM 设备不存在"
fi
echo ""

echo "3. 检查 DFA 应用..."
if adb shell pm list packages | grep -q com.dfa.app; then
    echo "   ✅ DFA 应用已安装"
    adb shell dumpsys package com.dfa.app | grep versionName
else
    echo "   ❌ DFA 应用未安装"
fi
echo ""

echo "4. 检查 VM 状态..."
dfa vm status 2>/dev/null || echo "   ❌ 无法获取 VM 状态"
echo ""

echo "5. 检查 Docker..."
dfa docker version 2>/dev/null || echo "   ❌ Docker 不可用"
echo ""

echo "6. 检查存储空间..."
adb shell df -h | grep -E "Filesystem|/data"
echo ""

echo "7. 检查内存..."
adb shell cat /proc/meminfo | grep -E "MemTotal|MemFree|MemAvailable"
echo ""

echo "=== 诊断完成 ==="
```

---

## 常见问题

### 安装问题

#### Q1: 安装失败，提示"INSTALL_FAILED_NO_MATCHING_ABIS"

**症状**：
```
Failure [INSTALL_FAILED_NO_MATCHING_ABIS: Failed to extract native libraries, res=-113]
```

**原因**：设备 CPU 架构不支持

**解决方案**：
```bash
# 检查设备架构
adb shell getprop ro.product.cpu.abi

# 应输出 arm64-v8a
# 如果输出其他架构，说明设备不支持 DFA
```

#### Q2: 安装失败，提示"INSTALL_FAILED_INSUFFICIENT_STORAGE"

**症状**：
```
Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]
```

**原因**：存储空间不足

**解决方案**：
```bash
# 检查存储空间
adb shell df -h /data

# 清理空间
adb shell pm trim-caches 1G

# 或卸载不用的应用
adb shell pm list packages -3
adb uninstall <package-name>
```

### 启动问题

#### Q3: 应用启动后闪退

**症状**：打开应用后立即闪退

**诊断步骤**：
```bash
# 查看崩溃日志
adb logcat -s AndroidRuntime:E | grep -A 20 "com.dfa.app"

# 查看系统日志
adb logcat -s DFA:V
```

**常见原因和解决方案**：

| 原因 | 解决方案 |
|------|----------|
| KVM 不可用 | 检查设备是否支持虚拟化 |
| 权限不足 | 授予应用必要权限 |
| 内存不足 | 关闭其他应用释放内存 |
| 数据损坏 | 清除应用数据重试 |

#### Q4: VM 启动超时

**症状**：
```
Error: VM startup timeout
```

**诊断步骤**：
```bash
# 检查 VM 日志
adb logcat -s VirtualizationService:V crosvm:V

# 检查资源使用
adb shell top -n 1 | grep crosvm

# 检查内存
adb shell cat /proc/meminfo | grep MemAvailable
```

**解决方案**：
```yaml
# 降低 VM 配置
vm:
  memory: 1024  # 降低内存
  cpus: 1       # 降低 CPU
```

### Docker 问题

#### Q5: Docker 命令无响应

**症状**：执行 `dfa docker` 命令后卡住

**诊断步骤**：
```bash
# 检查 Docker 服务状态
dfa docker info

# 检查 VM 状态
dfa vm status

# 检查通信通道
adb logcat -s VirtIO:V
```

**解决方案**：
```bash
# 重启 DFA 服务
adb shell am force-stop com.dfa.app
adb shell am start -n com.dfa.app/.MainActivity

# 如果问题持续，重启 VM
dfa vm restart
```

#### Q6: 容器启动失败

**症状**：
```
Error: failed to start container
```

**诊断步骤**：
```bash
# 查看容器日志
dfa docker logs <container-id>

# 查看详细错误
dfa docker inspect <container-id>

# 检查镜像
dfa docker images
```

**常见原因**：

| 原因 | 解决方案 |
|------|----------|
| 镜像不存在 | 先拉取镜像 `dfa docker pull <image>` |
| 资源不足 | 增加资源限制 |
| 端口冲突 | 更改端口映射 |
| 配置错误 | 检查容器配置 |

### 网络问题

#### Q7: 容器无法访问网络

**症状**：容器内无法访问外部网络

**诊断步骤**：
```bash
# 检查 VM 网络
dfa docker network ls

# 检查 DNS
dfa docker run --rm alpine nslookup google.com

# 检查网络连接
dfa docker run --rm alpine ping -c 3 8.8.8.8
```

**解决方案**：
```bash
# 重新创建网络
dfa docker network create --driver bridge newnet

# 使用自定义 DNS
dfa docker run --dns 8.8.8.8 <image>

# 检查防火墙设置
dfa network check
```

#### Q8: 端口映射不生效

**症状**：无法通过映射端口访问服务

**诊断步骤**：
```bash
# 检查端口映射
dfa docker port <container-id>

# 检查容器内服务
dfa docker exec <container-id> netstat -tlnp

# 检查端口占用
adb shell netstat -tlnp | grep <port>
```

**解决方案**：
```bash
# 确保容器内服务监听 0.0.0.0
# 而不是 127.0.0.1

# 检查端口转发规则
dfa port-forward list

# 重新创建端口映射
dfa docker run -d -p <host-port>:<container-port> <image>
```

---

## 错误代码参考

### VM 错误代码

| 错误码 | 名称 | 说明 | 解决方案 |
|--------|------|------|----------|
| 1001 | VM_CREATE_FAILED | VM 创建失败 | 检查配置和资源 |
| 1002 | VM_START_FAILED | VM 启动失败 | 查看日志定位原因 |
| 1003 | VM_STOP_FAILED | VM 停止失败 | 强制停止或重启 |
| 1004 | VM_TIMEOUT | VM 操作超时 | 增加超时时间 |
| 1005 | VM_INSUFFICIENT_MEMORY | 内存不足 | 降低内存配置 |
| 1006 | VM_INSUFFICIENT_STORAGE | 存储不足 | 清理存储空间 |
| 1007 | KVM_NOT_AVAILABLE | KVM 不可用 | 检查设备支持 |

### Docker 错误代码

| 错误码 | 名称 | 说明 | 解决方案 |
|--------|------|------|----------|
| 2001 | DOCKER_NOT_RUNNING | Docker 未运行 | 启动 Docker 服务 |
| 2002 | CONTAINER_CREATE_FAILED | 容器创建失败 | 检查镜像和配置 |
| 2003 | CONTAINER_START_FAILED | 容器启动失败 | 查看容器日志 |
| 2004 | IMAGE_PULL_FAILED | 镜像拉取失败 | 检查网络和仓库 |
| 2005 | NETWORK_ERROR | 网络错误 | 检查网络配置 |
| 2006 | VOLUME_ERROR | 存储卷错误 | 检查存储配置 |

### 通用错误代码

| 错误码 | 名称 | 说明 | 解决方案 |
|--------|------|------|----------|
| 3001 | PERMISSION_DENIED | 权限不足 | 授予必要权限 |
| 3002 | RESOURCE_EXHAUSTED | 资源耗尽 | 释放资源 |
| 3003 | INTERNAL_ERROR | 内部错误 | 查看日志 |
| 3004 | TIMEOUT | 操作超时 | 重试或增加超时 |
| 3005 | INVALID_CONFIG | 配置无效 | 检查配置文件 |

---

## 日志分析

### 日志位置

| 日志类型 | 位置 |
|----------|------|
| 应用日志 | `/data/data/com.dfa.app/files/logs/` |
| VM 日志 | `/data/data/com.dfa.app/files/vm/` |
| Docker 日志 | `/data/data/com.dfa.app/files/docker/` |

### 查看日志

```bash
# 实时查看应用日志
adb logcat -s DFA:V

# 查看 VM 日志
adb shell cat /data/data/com.dfa.app/files/vm/crosvm.log

# 查看 Docker 日志
adb shell cat /data/data/com.dfa.app/files/docker/daemon.log

# 导出所有日志
adb pull /data/data/com.dfa.app/files/logs/ ./dfa-logs/
```

### 日志分析示例

```
# 正常启动日志
[DFA] Initializing DFA service...
[DFA] Checking KVM availability... OK
[DFA] Creating VM instance...
[VM] Starting crosvm with config: memory=2048, cpus=2
[VM] VM started successfully
[DFA] Connecting to Docker...
[Docker] Docker daemon ready
[DFA] DFA service initialized successfully

# 异常日志示例
[DFA] Initializing DFA service...
[DFA] Checking KVM availability... FAILED
[DFA] Error: KVM device not found
[DFA] Please check if your device supports virtualization
```

---

## 解决方案

### 重置 DFA

```bash
# 停止服务
adb shell am force-stop com.dfa.app

# 清除数据
adb shell pm clear com.dfa.app

# 重新启动
adb shell am start -n com.dfa.app/.MainActivity
```

### 重新安装

```bash
# 卸载应用
adb uninstall com.dfa.app

# 清理残留
adb shell rm -rf /sdcard/DFA

# 重新安装
adb install dfa.apk
```

### 恢复出厂设置

```bash
# 完全重置
dfa factory-reset

# 或手动操作
adb shell am force-stop com.dfa.app
adb shell rm -rf /data/data/com.dfa.app
adb shell rm -rf /sdcard/DFA
adb uninstall com.dfa.app
adb install dfa.apk
```

---

## 获取帮助

### 提交 Issue

在提交 Issue 前，请收集以下信息：

```bash
# 运行诊断脚本
./dfa-diagnose.sh > diagnose-report.txt

# 导出日志
adb pull /data/data/com.dfa.app/files/logs/ ./logs/

# 获取系统信息
adb shell getprop > system-info.txt
```

### Issue 模板

```markdown
## 问题描述
[简要描述问题]

## 环境信息
- 设备型号：
- Android 版本：
- DFA 版本：
- 问题发生时间：

## 复现步骤
1. 
2. 
3. 

## 期望结果
[描述期望的行为]

## 实际结果
[描述实际发生的情况]

## 日志
[粘贴相关日志]

## 截图
[如有必要，添加截图]
```

### 联系方式

- GitHub Issues: https://github.com/your-org/dfa/issues
- 文档: https://dfa-docs.example.com
- 社区: https://community.dfa.example.com

---

## 相关文档

- [安装指南](INSTALLATION.md)
- [FAQ](FAQ.md)
- [AVF 指南](AVF-GUIDE.md)
- [Docker 集成](DOCKER-INTEGRATION.md)