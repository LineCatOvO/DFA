# Task-024: 网络端口转发实现

**创建时间**：2026-03-15
**更新时间**：2026-03-16
**优先级**：P0（MVP核心）
**状态**：待处理
**MVP阶段**：阶段3 - 端到端集成
**依赖**：Task-023
**完成时间**：

## 任务描述
实现Android与QEMU虚拟机中Docker容器的网络端口转发，支持从Android设备访问容器服务。

## 背景
架构重构后，DFA项目使用SSH/Socket通信替代VirtIO网络。需要实现新的端口转发机制。

## 架构变更说明
- **原方案**：基于VirtIO网络设备实现桥接
- **新方案**：基于QEMU用户模式网络和SSH隧道

## 执行计划
- [ ] 设计网络架构
- [ ] 实现QEMU用户模式网络配置
- [ ] 实现QEMU端口转发（hostfwd）
- [ ] 实现SSH隧道端口转发
- [ ] 实现动态端口分配
- [ ] 测试网络连通性

## 知识点记录
### 技术要点
- QEMU用户模式网络：-netdev user,id=net0,hostfwd=tcp::8080-:80
- SSH隧道：ssh -L local_port:remote_host:remote_port
- 端口转发类型：本地转发、远程转发
- 动态端口分配：避免端口冲突

### 注意事项
- 支持TCP和UDP协议
- 处理端口冲突
- 实现动态端口分配
- 考虑IPv4和IPv6双栈
- SSH连接断开时自动重连

## 验收标准
- [ ] 容器端口可通过Android访问
- [ ] 支持TCP端口转发
- [ ] 支持UDP端口转发（可选）
- [ ] 网络延迟<100ms
- [ ] 支持多容器同时运行
- [ ] 端口冲突检测和动态分配

## 相关资源
- [QEMU用户模式网络](https://wiki.qemu.org/Documentation/Networking#User_Networking)
- [SSH隧道教程](https://www.ssh.com/academy/ssh/tunneling/example)
- [SshChannelImpl.kt](../core/vm/src/main/java/com/dfa/core/vm/channel/SshChannelImpl.kt)
- [QemuConfig.kt](../core/vm/src/main/java/com/dfa/core/vm/qemu/QemuConfig.kt)