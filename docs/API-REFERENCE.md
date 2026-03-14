# API 参考文档

本文档提供 DFA 项目的 API 接口参考。

---

## 目录

- [API 概述](#api-概述)
- [认证与授权](#认证与授权)
- [VM 管理 API](#vm-管理-api)
- [Docker API](#docker-api)
- [网络 API](#网络-api)
- [存储 API](#存储-api)
- [系统 API](#系统-api)
- [错误处理](#错误处理)

---

## API 概述

### API 架构

```mermaid
graph TB
    subgraph "客户端"
        A[CLI]
        B[GUI]
        C[Web]
    end
    
    subgraph "API 层"
        D[REST API]
        E[gRPC API]
        F[WebSocket]
    end
    
    subgraph "服务层"
        G[VM Service]
        H[Docker Service]
        I[Network Service]
        J[Storage Service]
    end
    
    A --> D
    B --> D
    C --> D
    
    D --> G
    D --> H
    E --> I
    E --> J
    F --> G
```

### API 端点

| 服务 | 端点 | 协议 |
|------|------|------|
| REST API | `http://localhost:8080/api/v1` | HTTP/REST |
| gRPC API | `localhost:50051` | gRPC |
| WebSocket | `ws://localhost:8080/ws` | WebSocket |

### API 版本

当前 API 版本: `v1`

版本策略:
- 向后兼容的变更不会增加主版本号
- 新增功能会增加次版本号
- Bug 修复会增加修订号

---

## 认证与授权

### 认证方式

#### Token 认证

```bash
# 获取 Token
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# 响应
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expires_in": 3600
}

# 使用 Token
curl -X GET http://localhost:8080/api/v1/vms \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

#### API Key 认证

```bash
# 使用 API Key
curl -X GET http://localhost:8080/api/v1/vms \
  -H "X-API-Key: your-api-key"
```

### 权限模型

| 角色 | 权限 |
|------|------|
| admin | 所有权限 |
| operator | VM 和容器管理 |
| viewer | 只读访问 |

---

## VM 管理 API

### 获取 VM 列表

```http
GET /api/v1/vms
```

**响应**:
```json
{
  "vms": [
    {
      "id": "vm-001",
      "name": "default",
      "status": "running",
      "config": {
        "memory": 2048,
        "cpus": 2,
        "storage": 10
      },
      "created_at": "2024-01-15T10:00:00Z",
      "updated_at": "2024-01-15T10:00:00Z"
    }
  ],
  "total": 1
}
```

### 创建 VM

```http
POST /api/v1/vms
Content-Type: application/json

{
  "name": "my-vm",
  "config": {
    "memory": 2048,
    "cpus": 2,
    "storage": 10,
    "protected": true
  }
}
```

**响应**:
```json
{
  "id": "vm-002",
  "name": "my-vm",
  "status": "creating",
  "config": {
    "memory": 2048,
    "cpus": 2,
    "storage": 10,
    "protected": true
  },
  "created_at": "2024-01-15T11:00:00Z"
}
```

### 获取 VM 详情

```http
GET /api/v1/vms/{vm_id}
```

**响应**:
```json
{
  "id": "vm-001",
  "name": "default",
  "status": "running",
  "config": {
    "memory": 2048,
    "cpus": 2,
    "storage": 10,
    "protected": true
  },
  "stats": {
    "memory_used": 1024,
    "cpu_usage": 25.5,
    "storage_used": 5.2
  },
  "created_at": "2024-01-15T10:00:00Z",
  "updated_at": "2024-01-15T10:00:00Z"
}
```

### 启动 VM

```http
POST /api/v1/vms/{vm_id}/start
```

**响应**:
```json
{
  "id": "vm-001",
  "status": "starting",
  "message": "VM is starting"
}
```

### 停止 VM

```http
POST /api/v1/vms/{vm_id}/stop
```

**请求体**:
```json
{
  "force": false,
  "timeout": 30
}
```

**响应**:
```json
{
  "id": "vm-001",
  "status": "stopping",
  "message": "VM is stopping"
}
```

### 删除 VM

```http
DELETE /api/v1/vms/{vm_id}
```

**响应**:
```json
{
  "id": "vm-001",
  "status": "deleted",
  "message": "VM deleted successfully"
}
```

---

## Docker API

### 容器操作

#### 列出容器

```http
GET /api/v1/containers
```

**参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| all | boolean | 显示所有容器 |
| limit | integer | 限制数量 |
| filters | string | 过滤条件 |

**响应**:
```json
{
  "containers": [
    {
      "id": "container-001",
      "name": "nginx",
      "image": "nginx:latest",
      "status": "running",
      "state": "running",
      "ports": [
        {
          "host_port": 8080,
          "container_port": 80,
          "protocol": "tcp"
        }
      ],
      "created_at": "2024-01-15T10:00:00Z"
    }
  ],
  "total": 1
}
```

#### 创建容器

```http
POST /api/v1/containers
Content-Type: application/json

{
  "name": "my-nginx",
  "image": "nginx:latest",
  "ports": [
    {
      "host_port": 8080,
      "container_port": 80
    }
  ],
  "env": ["NGINX_PORT=80"],
  "volumes": [
    {
      "source": "mydata",
      "destination": "/data"
    }
  ],
  "resources": {
    "memory": "512m",
    "cpus": "1.0"
  }
}
```

**响应**:
```json
{
  "id": "container-002",
  "name": "my-nginx",
  "status": "created",
  "warnings": []
}
```

#### 启动容器

```http
POST /api/v1/containers/{container_id}/start
```

#### 停止容器

```http
POST /api/v1/containers/{container_id}/stop

{
  "timeout": 10
}
```

#### 删除容器

```http
DELETE /api/v1/containers/{container_id}

{
  "force": false,
  "volumes": true
}
```

#### 查看容器日志

```http
GET /api/v1/containers/{container_id}/logs

{
  "follow": false,
  "tail": 100,
  "timestamps": true
}
```

**响应**:
```json
{
  "logs": [
    {
      "timestamp": "2024-01-15T10:00:00Z",
      "stream": "stdout",
      "message": "Server started on port 80"
    }
  ]
}
```

#### 在容器中执行命令

```http
POST /api/v1/containers/{container_id}/exec

{
  "cmd": ["ls", "-la"],
  "env": [],
  "tty": true
}
```

### 镜像操作

#### 列出镜像

```http
GET /api/v1/images
```

**响应**:
```json
{
  "images": [
    {
      "id": "image-001",
      "name": "nginx",
      "tag": "latest",
      "size": 142000000,
      "created_at": "2024-01-10T00:00:00Z"
    }
  ],
  "total": 1
}
```

#### 拉取镜像

```http
POST /api/v1/images/pull

{
  "name": "nginx",
  "tag": "latest",
  "platform": "linux/arm64"
}
```

#### 删除镜像

```http
DELETE /api/v1/images/{image_id}

{
  "force": false
}
```

---

## 网络 API

### 列出网络

```http
GET /api/v1/networks
```

**响应**:
```json
{
  "networks": [
    {
      "id": "network-001",
      "name": "bridge",
      "driver": "bridge",
      "subnet": "172.17.0.0/16",
      "gateway": "172.17.0.1",
      "containers": ["container-001", "container-002"]
    }
  ]
}
```

### 创建网络

```http
POST /api/v1/networks

{
  "name": "my-network",
  "driver": "bridge",
  "subnet": "172.20.0.0/16",
  "gateway": "172.20.0.1"
}
```

### 删除网络

```http
DELETE /api/v1/networks/{network_id}
```

---

## 存储 API

### 列出数据卷

```http
GET /api/v1/volumes
```

**响应**:
```json
{
  "volumes": [
    {
      "name": "mydata",
      "driver": "local",
      "mountpoint": "/var/lib/docker/volumes/mydata/_data",
      "size": 1024000,
      "created_at": "2024-01-15T10:00:00Z"
    }
  ],
  "total": 1
}
```

### 创建数据卷

```http
POST /api/v1/volumes

{
  "name": "mydata",
  "driver": "local",
  "driver_opts": {
    "type": "tmpfs",
    "device": "tmpfs",
    "o": "size=100m"
  }
}
```

### 删除数据卷

```http
DELETE /api/v1/volumes/{volume_name}

{
  "force": false
}
```

---

## 系统 API

### 获取系统信息

```http
GET /api/v1/system/info
```

**响应**:
```json
{
  "version": "1.0.0",
  "os": "Android 14",
  "arch": "arm64",
  "vm": {
    "status": "running",
    "memory": 2048,
    "cpus": 2
  },
  "docker": {
    "version": "24.0.5",
    "containers": 5,
    "images": 10
  },
  "resources": {
    "memory_total": 8192,
    "memory_used": 4096,
    "storage_total": 64000,
    "storage_used": 32000
  }
}
```

### 获取系统状态

```http
GET /api/v1/system/status
```

**响应**:
```json
{
  "status": "healthy",
  "components": {
    "vm": "healthy",
    "docker": "healthy",
    "network": "healthy",
    "storage": "healthy"
  },
  "uptime": 86400
}
```

### 获取版本信息

```http
GET /api/v1/system/version
```

**响应**:
```json
{
  "version": "1.0.0",
  "commit": "abc123",
  "build_date": "2024-01-15T00:00:00Z",
  "go_version": "go1.21.0"
}
```

---

## 错误处理

### 错误响应格式

```json
{
  "error": {
    "code": "VM_NOT_FOUND",
    "message": "VM with id 'vm-xxx' not found",
    "details": {
      "vm_id": "vm-xxx"
    }
  }
}
```

### 错误代码

| 代码 | HTTP 状态码 | 说明 |
|------|-------------|------|
| INVALID_REQUEST | 400 | 请求参数无效 |
| UNAUTHORIZED | 401 | 未授权 |
| FORBIDDEN | 403 | 权限不足 |
| NOT_FOUND | 404 | 资源不存在 |
| CONFLICT | 409 | 资源冲突 |
| INTERNAL_ERROR | 500 | 内部错误 |
| SERVICE_UNAVAILABLE | 503 | 服务不可用 |

### VM 相关错误

| 代码 | 说明 |
|------|------|
| VM_NOT_FOUND | VM 不存在 |
| VM_ALREADY_EXISTS | VM 已存在 |
| VM_START_FAILED | VM 启动失败 |
| VM_STOP_FAILED | VM 停止失败 |
| VM_INSUFFICIENT_RESOURCES | 资源不足 |

### 容器相关错误

| 代码 | 说明 |
|------|------|
| CONTAINER_NOT_FOUND | 容器不存在 |
| CONTAINER_ALREADY_RUNNING | 容器已在运行 |
| CONTAINER_START_FAILED | 容器启动失败 |
| IMAGE_NOT_FOUND | 镜像不存在 |
| IMAGE_PULL_FAILED | 镜像拉取失败 |

---

## SDK 示例

### Kotlin SDK

```kotlin
// 创建 DFA 客户端
val client = DfaClient.Builder()
    .baseUrl("http://localhost:8080")
    .apiKey("your-api-key")
    .build()

// 获取 VM 列表
val vms = client.vm.list()
vms.forEach { vm ->
    println("VM: ${vm.name} - ${vm.status}")
}

// 创建容器
val container = client.container.create(
    ContainerCreateRequest(
        name = "my-nginx",
        image = "nginx:latest",
        ports = listOf(
            PortMapping(hostPort = 8080, containerPort = 80)
        )
    )
)
println("Created container: ${container.id}")
```

### Python SDK

```python
from dfa import DfaClient

# 创建客户端
client = DfaClient(
    base_url="http://localhost:8080",
    api_key="your-api-key"
)

# 获取容器列表
containers = client.container.list()
for c in containers:
    print(f"Container: {c.name} - {c.status}")

# 运行容器
container = client.container.run(
    image="nginx:latest",
    name="my-nginx",
    ports={"8080": "80"}
)
print(f"Started container: {container.id}")
```

---

## 相关文档

- [架构文档](ARCHITECTURE.md)
- [开发指南](DEVELOPMENT.md)
- [Docker 集成](DOCKER-INTEGRATION.md)