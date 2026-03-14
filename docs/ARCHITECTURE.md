# DFA 技术架构文档

本文档详细描述 DFA（Docker For Android）项目的技术架构设计。

---

## 目录

- [概述](#概述)
- [AVF 架构基础](#avf-架构基础)
- [DFA 系统架构](#dfa-系统架构)
- [核心组件](#核心组件)
- [数据流](#数据流)
- [安全架构](#安全架构)
- [性能架构](#性能架构)

---

## 概述

DFA 利用 Android 的 AVF（Android Virtualization Framework）在 Android 设备上创建隔离的虚拟机环境，并在其中运行 Docker 引擎，从而实现原生 Docker 容器支持。

### 设计目标

| 目标 | 说明 |
|------|------|
| 安全隔离 | 容器与宿主系统完全隔离 |
| 性能优化 | 最小化虚拟化开销 |
| 易用性 | 提供直观的用户界面 |
| 兼容性 | 支持标准 Docker 镜像和命令 |
| 可扩展 | 支持插件和扩展机制 |

---

## AVF 架构基础

### 什么是 AVF

Android Virtualization Framework (AVF) 是 Android 13 引入的虚拟化框架，允许在 Android 设备上运行受保护的虚拟机。

```mermaid
graph TB
    subgraph "Android 系统"
        A[Android Framework] --> B[AVF Service]
        B --> C[Virtualization Service]
        C --> D[Hypervisor]
        D --> E[Protected VM]
        D --> F[Non-Protected VM]
    end
    
    subgraph "硬件层"
        G[ARM CPU] --> H[KVM]
        H --> D
    end
```

### AVF 核心概念

| 概念 | 说明 |
|------|------|
| Protected VM (pVM) | 受保护的虚拟机，与宿主系统隔离 |
| Non-Protected VM | 非保护虚拟机，性能更高但隔离性较弱 |
| Hypervisor | 管理虚拟机的软件层 |
| Crosvm | Chrome OS 的虚拟机监视器，AVF 使用其变体 |
| VirtIO | 虚拟设备接口标准 |

### AVF 组件

```mermaid
graph LR
    subgraph "AVF 组件"
        A[android.system.virtualizationservice]
        B[android.system.virtualmachine]
        C[virtmgr]
        D[crosvm]
    end
    
    A --> B
    B --> C
    C --> D
```

---

## DFA 系统架构

### 整体架构

```mermaid
graph TB
    subgraph "用户层"
        A[GUI 应用]
        B[CLI 工具]
        C[Web 控制台]
    end
    
    subgraph "DFA 核心层"
        D[DFA Service]
        E[VM Manager]
        F[Docker Bridge]
        G[Image Manager]
        H[Network Manager]
    end
    
    subgraph "AVF 层"
        I[Virtualization Service]
        J[VM Instance]
    end
    
    subgraph "容器层"
        K[Docker Engine]
        L[Container Runtime]
        M[Containers]
    end
    
    subgraph "存储层"
        N[Image Storage]
        O[Volume Storage]
        P[Config Storage]
    end
    
    A --> D
    B --> D
    C --> D
    
    D --> E
    D --> F
    D --> G
    D --> H
    
    E --> I
    I --> J
    J --> K
    
    K --> L
    L --> M
    
    G --> N
    F --> O
    D --> P
```

### 架构层次

| 层次 | 组件 | 职责 |
|------|------|------|
| 用户层 | GUI/CLI/Web | 用户交互界面 |
| 核心层 | DFA Service | 核心业务逻辑 |
| 虚拟化层 | AVF | 虚拟机管理 |
| 容器层 | Docker | 容器运行时 |
| 存储层 | Storage | 数据持久化 |

---

## 核心组件

### 1. DFA Service

DFA 的核心服务，负责协调各组件工作。

```java
// DFA Service 核心接口
public interface DfaService {
    // 生命周期管理
    void initialize(Context context);
    void start();
    void stop();
    
    // VM 管理
    VmInstance createVm(VmConfig config);
    void destroyVm(VmInstance vm);
    
    // Docker 操作
    DockerClient getDockerClient();
}
```

### 2. VM Manager

管理虚拟机的创建、启动、停止和销毁。

```mermaid
sequenceDiagram
    participant App as DFA App
    participant VM as VM Manager
    participant AVF as AVF Service
    participant VMInst as VM Instance
    
    App->>VM: createVm(config)
    VM->>AVF: createVirtualMachine(config)
    AVF->>VMInst: 创建 VM
    VMInst-->>AVF: VM 就绪
    AVF-->>VM: VM 实例
    VM-->>App: 返回 VM 句柄
    
    App->>VM: startVm()
    VM->>AVF: start()
    AVF->>VMInst: 启动 VM
    VMInst-->>AVF: 运行中
    AVF-->>VM: 启动成功
    VM-->>App: 操作完成
```

### 3. Docker Bridge

Docker 与 Android 系统的桥接层。

```mermaid
graph LR
    subgraph "Android"
        A[DFA App]
        B[Docker Bridge]
    end
    
    subgraph "VM"
        C[Docker Daemon]
        D[Docker CLI]
    end
    
    A -->|gRPC/Socket| B
    B -->|VirtIO Channel| C
    C --> D
```

### 4. Image Manager

管理 Docker 镜像的拉取、存储和分发。

```mermaid
graph TB
    A[Image Manager]
    B[Registry Client]
    C[Layer Store]
    D[Image Cache]
    E[Local Registry]
    
    A --> B
    A --> C
    A --> D
    A --> E
    
    B -->|pull| F[Docker Hub]
    B -->|pull| G[Private Registry]
```

### 5. Network Manager

管理容器网络配置。

```mermaid
graph TB
    A[Network Manager]
    B[Bridge Network]
    C[Host Network]
    D[Port Forwarding]
    E[DNS Resolver]
    
    A --> B
    A --> C
    A --> D
    A --> E
    
    B --> F[Container 1]
    B --> G[Container 2]
```

---

## 数据流

### 容器创建流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant App as DFA App
    participant VM as VM Manager
    participant Docker as Docker Engine
    participant Container as Container
    
    User->>App: docker run nginx
    App->>VM: 检查 VM 状态
    VM-->>App: VM 运行中
    App->>Docker: 发送创建请求
    Docker->>Docker: 拉取镜像（如需要）
    Docker->>Container: 创建容器
    Container-->>Docker: 容器就绪
    Docker-->>App: 容器 ID
    App-->>User: 显示容器信息
```

### 镜像拉取流程

```mermaid
sequenceDiagram
    participant App as DFA App
    participant IM as Image Manager
    participant Cache as Image Cache
    participant Registry as Docker Registry
    
    App->>IM: pull image:nginx
    IM->>Cache: 检查本地缓存
    alt 镜像存在
        Cache-->>IM: 返回镜像
    else 镜像不存在
        IM->>Registry: 请求镜像清单
        Registry-->>IM: 返回清单
        IM->>Registry: 下载镜像层
        Registry-->>IM: 镜像层数据
        IM->>Cache: 存储镜像
    end
    IM-->>App: 镜像就绪
```

---

## 安全架构

### 安全模型

```mermaid
graph TB
    subgraph "安全边界"
        A[Android 系统]
        B[AVF 隔离层]
        C[Protected VM]
        D[Docker 容器]
    end
    
    A -->|硬件隔离| B
    B -->|虚拟化隔离| C
    C -->|容器隔离| D
```

### 安全层次

| 层次 | 机制 | 说明 |
|------|------|------|
| 硬件层 | ARM TrustZone | 硬件级安全隔离 |
| 虚拟化层 | AVF Protected VM | 虚拟机级隔离 |
| 容器层 | Docker Namespaces | 容器级隔离 |
| 应用层 | SELinux/AppArmor | 强制访问控制 |

### 安全特性

1. **内存隔离**：虚拟机拥有独立的内存空间
2. **存储隔离**：容器数据存储在虚拟机磁盘镜像中
3. **网络隔离**：虚拟机网络与宿主网络隔离
4. **权限控制**：细粒度的权限管理机制

---

## 性能架构

### 性能优化策略

```mermaid
graph LR
    A[性能优化]
    B[启动优化]
    C[运行优化]
    D[存储优化]
    E[网络优化]
    
    A --> B
    A --> C
    A --> D
    A --> E
    
    B --> B1[预加载 VM]
    B --> B2[延迟初始化]
    
    C --> C1[资源池化]
    C --> C2[缓存策略]
    
    D --> D1[分层存储]
    D --> D2[压缩传输]
    
    E --> E1[连接复用]
    E --> E2[数据压缩]
```

### 资源管理

| 资源 | 管理策略 |
|------|----------|
| CPU | 动态分配，按需调整 |
| 内存 | 预留 + 动态扩展 |
| 存储 | 分层存储，自动清理 |
| 网络 | 按需分配，带宽限制 |

---

## 扩展架构

### 插件系统

```mermaid
graph TB
    A[DFA Core]
    B[Plugin Manager]
    C[Plugin API]
    
    D[Network Plugin]
    E[Storage Plugin]
    F[Auth Plugin]
    G[Custom Plugin]
    
    A --> B
    B --> C
    
    C --> D
    C --> E
    C --> F
    C --> G
```

### 扩展点

| 扩展点 | 说明 |
|--------|------|
| Network Driver | 自定义网络驱动 |
| Storage Driver | 自定义存储驱动 |
| Auth Provider | 自定义认证提供者 |
| Log Driver | 自定义日志驱动 |
| Metric Collector | 自定义指标收集器 |

---

## 相关文档

- [安装指南](INSTALLATION.md)
- [开发指南](DEVELOPMENT.md)
- [AVF 指南](AVF-GUIDE.md)
- [Docker 集成](DOCKER-INTEGRATION.md)
- [安全指南](SECURITY.md)