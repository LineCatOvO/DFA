# Task-019: Docker API交互实现

**创建时间**：2026-03-15
**更新时间**：2026-03-16
**优先级**：P0（MVP核心）
**状态**：待处理
**MVP阶段**：阶段2 - Docker集成
**依赖**：Task-018
**完成时间**：

## 任务描述
实现DockerClient接口，完成与Docker守护进程的API交互逻辑。

## 背景
架构重构后，Docker API需要通过SSH隧道连接到QEMU虚拟机中的Docker守护进程。

## 架构变更说明
- **原方案**：直接通过Unix Socket连接本地Docker
- **新方案**：通过SSH隧道连接远程Docker守护进程

## 执行计划
- [ ] 实现SSH隧道连接（端口转发）
- [ ] 实现HTTP请求封装
- [ ] 实现容器操作API
- [ ] 实现镜像操作API
- [ ] 实现网络操作API
- [ ] 实现事件监听
- [ ] 编写单元测试

## 知识点记录
### 技术要点
- SSH隧道：通过SSH转发Docker API端口
- HTTP客户端：OkHttp
- JSON序列化：kotlinx.serialization
- 事件流：Server-Sent Events

### 注意事项
- 需要通过SSH访问VM内的Docker Socket
- 连接池管理
- 错误重试策略
- SSH连接断开时自动重连

## 验收标准
- [ ] 成功连接Docker守护进程（通过SSH隧道）
- [ ] 容器操作API全部实现
- [ ] 镜像操作API全部实现
- [ ] 网络操作API全部实现
- [ ] 事件监听正常工作
- [ ] 单元测试覆盖率≥80%

## 相关资源
- [Docker Engine API文档](https://docs.docker.com/engine/api/)
- [OkHttp](https://square.github.io/okhttp/)
- [SshChannelImpl.kt](../core/vm/src/main/java/com/dfa/core/vm/channel/SshChannelImpl.kt)
- [DockerClient.kt](../core/docker/src/main/java/com/dfa/core/docker/DockerClient.kt)