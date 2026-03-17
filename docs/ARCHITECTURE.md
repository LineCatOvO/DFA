# CDroid 技术架构文档

本文档详细描述 CDroid（Container Dashboard）项目的技术架构设计。

---

## 目录

- [概述](#概述)
- [CDroid 系统架构](#cdroid-系统架构)
- [核心组件](#核心组件)
- [数据流](#数据流)
- [安全架构](#安全架构)
- [性能架构](#性能架构)

---

## 概述

CDroid 通过 Docker Context 和容器服务 API 管理远程或本地的 Docker、Podman 等容器服务。通过统一的用户界面，提供跨平台的容器管理能力。

### 设计目标

| 目标 | 说明 |
|------|------|
| 跨平台支持 | 支持Android、iOS、Web等多平台 |
| 多服务支持 | 支持Docker、Podman等多种容器服务 |
| 远程管理 | 支持连接和管理远程容器服务 |
| 易用性 | 提供直观的用户界面 |
| 可扩展 | 支持插件和扩展机制 |

---

## CDroid 系统架构

### 整体架构

```mermaid
graph TB
    subgraph "用户层"
        A[Android App]
        B[iOS App]
        C[Web Console]
    end
    
    subgraph "CDroid 核心层"
        D[Context Manager]
        E[Docker Provider]
        F[Podman Provider]
        G[Image Manager]
        H[Network Manager]
    end
    
    subgraph "容器服务层"
        I[Docker Context API]
        J[Podman API]
        K[Remote Docker Service]
        L[Local Docker Service]
    end
    
    subgraph "存储层"
        M[Config Storage]
        N[Cache Storage]
    end
    
    A --> D
    B --> D
    C --> D
    
    D --> E
    D --> F
    D --> G
    D --> H
    
    E --> I
    F --> J
    I --> K
    I --> L
    
    D --> M
    G --> N
```

### 架构层次

| 层次 | 组件 | 职责 |
|------|------|------|
| 用户层 | Android/iOS/Web | 用户交互界面 |
| 核心层 | CDroid Service | 核心业务逻辑 |
| 服务层 | Docker/Podman API | 容器服务接口 |
| 存储层 | Storage | 数据持久化 |

---

## 核心组件

### 1. Context Manager

Context Manager负责管理Docker Context的配置、切换和连接。

```kotlin
// Context Manager 核心接口
interface ContextManager {
    // 生命周期管理
    suspend fun initialize(context: Context)
    suspend fun start()
    suspend fun stop()
    
    // Context 管理
    suspend fun createContext(config: ContextConfig): Context
    suspend fun destroyContext(contextId: String)
    suspend fun switchContext(contextId: String)
    suspend fun listContexts(): List<Context>
    
    // Provider 管理
    suspend fun getProvider(contextId: String): ContainerProvider
}
```

### 2. Docker Provider

Docker Provider提供Docker服务的API封装和操作功能。

```mermaid
sequenceDiagram
    participant App as CDroid App
    participant CM as Context Manager
    participant DP as Docker Provider
    participant API as Docker API
    
    App->>CM: switchContext("docker-local")
    CM->>DP: getProvider("docker-local")
    DP->>API: 连接Docker服务
    API-->>DP: 连接成功
    DP-->>CM: 返回Provider实例
    CM-->>App: Context切换完成
    
    App->>DP: listContainers()
    DP->>API: GET /containers/json
    API-->>DP: 容器列表
    DP-->>App: 返回容器信息
```

### 3. Podman Provider

Podman Provider提供Podman服务的API封装和操作功能。

```mermaid
graph LR
    subgraph "CDroid"
        A[Context Manager]
        B[Podman Provider]
    end
    
    subgraph "Podman Service"
        C[Podman API]
        D[Podman Daemon]
    end
    
    A --> B
    B -->|HTTP/Unix Socket| C
    C --> D
```

### 4. Image Manager

管理容器镜像的拉取、存储和分发。

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
    participant App as CDroid App
    participant CM as Context Manager
    participant Provider as Container Provider
    participant Container as Container
    
    User->>App: docker run nginx
    App->>CM: 检查Context状态
    CM-->>App: Context就绪
    App->>Provider: 发送创建请求
    Provider->>Provider: 拉取镜像（如需要）
    Provider->>Container: 创建容器
    Container-->>Provider: 容器就绪
    Provider-->>App: 容器ID
    App-->>User: 显示容器信息
```

### 镜像拉取流程

```mermaid
sequenceDiagram
    participant App as CDroid App
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
        A[CDroid App]
        B[Context Manager]
        C[Container Service]
        D[Container]
    end
    
    A -->|加密连接| B
    B -->|认证授权| C
    C -->|容器隔离| D
```

### 安全层次

| 层次 | 机制 | 说明 |
|------|------|------|
| 传输层 | TLS/SSL | 加密通信 |
| 认证层 | Token/Cert | 身份认证 |
| 授权层 | RBAC | 访问控制 |
| 容器层 | Namespaces | 容器级隔离 |

### 安全特性

1. **加密通信**：所有与容器服务的通信都使用TLS加密
2. **认证授权**：支持多种认证方式（Token、证书、OAuth）
3. **权限控制**：细粒度的权限管理机制
4. **审计日志**：记录所有操作日志

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
    A[CDroid Core]
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