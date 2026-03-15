# Task-021: AVF API集成

**创建时间**：2026-03-15
**优先级**：P0（MVP核心）
**状态**：已完成
**完成时间**：2026-03-15
**MVP阶段**：阶段1 - AVF集成

## 任务描述
集成Android Virtualization Framework API，实现真实的虚拟机创建和管理能力。这是MVP的关键阻塞任务。

## 背景
当前AvfVmAdapterImpl是模拟实现，没有调用实际的AVF API，导致无法创建真实的虚拟机。

## 执行计划
- [ ] 研究AVF API文档和示例
- [ ] 添加AVF依赖到build.gradle
- [ ] 创建VirtualizationManager封装
- [ ] 实现虚拟机配置构建器
- [ ] 实现虚拟机生命周期回调
- [ ] 编写集成测试

## 知识点记录
### 技术要点
- Android 13+ AVF API
- VirtualMachineManager：虚拟机管理器
- VirtualMachineConfig：虚拟机配置
- VirtualMachineCallback：生命周期回调
- Protected VM vs Non-Protected VM

### 注意事项
- 需要Android 13+设备
- 需要设备支持AVF
- 需要申请必要权限
- 注意内存和CPU资源限制

## 验收标准
- [ ] 成功调用AVF API创建虚拟机
- [ ] 虚拟机可以启动和停止
- [ ] 可以获取虚拟机状态
- [ ] 集成测试通过

## 相关资源
- [AVF官方文档](https://source.android.com/docs/core/architecture/virtualization/avf)
- [AVF示例代码](https://android.googlesource.com/platform/packages/modules/Virtualization/)
- [Microdroid文档](https://android.googlesource.com/platform/packages/modules/Virtualization/+/refs/heads/master/microdroid/)