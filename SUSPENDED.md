# DFA 项目搁置说明

**搁置时间**：2026-03-15
**搁置状态**：无限期搁置

---

## 搁置原因

本项目已于 2026-03-15 决定无限期搁置，主要原因如下：

### 1. AVF SDK 不对开发者开放

Android Virtualization Framework (AVF) 是 Android 13 引入的系统级虚拟化框架，但 Google 并未向普通开发者开放相关 SDK：

- `android.system.virtualizationservice` API 仅限系统应用使用
- 普通应用无法获取 `VirtualMachineManager` 系统服务
- 没有公开的开发者文档和示例代码

### 2. 无法在标准用户环境使用

即使设备支持 AVF，普通用户环境也无法使用：

- 需要系统级权限（`android.permission.MANAGE_VIRTUAL_MACHINE`）
- 需要 root 或系统签名
- 普通应用无法绑定虚拟机服务

---

## 已完成工作

尽管项目搁置，以下技术成果已完成并保留：

### Phase 1: 基础架构
- ✅ Task-001: 项目初始化与开发环境搭建
- ✅ Task-002: AVF虚拟机管理模块
- ✅ Task-003: 镜像下载管理模块
- ✅ Task-004: VirtIO通信层
- ✅ Task-005: 存储管理模块

### Phase 2: 编译修复
- ✅ Task-009~013: 编译错误修复系列

### Phase 3: MVP核心（代码已实现，无法验证）
- ✅ Task-021: AVF API集成
- ✅ Task-022: AvfVmAdapter真实实现
- ✅ Task-018: DockerClient接口设计

---

## 技术成果

### 代码统计
- 文件数：135+
- 代码行数：26,000+
- 主要语言：Kotlin

### 核心模块
| 模块 | 说明 | 完成度 |
|------|------|--------|
| core/vm | 虚拟机管理 | 90% |
| core/docker | Docker集成 | 30% |
| core/common | 公共组件 | 100% |
| app | Android应用 | 60% |

### 关键技术实现
1. **AvfManager** - VirtualMachineManager 封装
2. **VmConfigBuilder** - VM 配置构建器
3. **AvfVmCallbackImpl** - 生命周期回调实现
4. **DockerClient** - Docker 客户端接口
5. **VirtIO通信层** - VM与宿主机通信

---

## 未来展望

### 恢复条件
项目可能在以下情况下恢复：

1. **Google 开放 AVF SDK**：如果 Google 向开发者开放 AVF API
2. **替代技术方案**：出现其他可行的 Android 虚拟化方案
3. **系统级合作**：与设备厂商合作获取系统权限

### 技术参考价值
即使项目搁置，代码和架构设计仍具有参考价值：

- Kotlin + Coroutines 异步架构
- Hilt 依赖注入模式
- Repository Pattern 接口设计
- VirtIO 设备通信实现

---

## 相关资源

- [AVF 官方文档](https://source.android.com/docs/core/architecture/virtualization/avf)
- [项目架构文档](./docs/ARCHITECTURE.md)
- [AVF 指南](./docs/AVF-GUIDE.md)

---

*最后更新：2026-03-15*