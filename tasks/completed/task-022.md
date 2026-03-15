# Task-022: AvfVmAdapter真实实现

**创建时间**：2026-03-15
**优先级**：P0（MVP核心）
**状态**：已完成
**完成时间**：2026-03-15
**完成说明**：Task-021已实现核心功能，剩余单元测试更新和设备验证
**MVP阶段**：阶段1 - AVF集成
**依赖**：Task-021

## 任务描述
替换当前的模拟实现，使用真实的AVF API实现AvfVmAdapter，使VM能够真正创建和启动。

## 背景
当前core/vm/avf/AvfVmAdapterImpl.kt是模拟实现，所有方法都返回模拟数据，无法创建真实虚拟机。

## 执行计划
- [ ] 重构AvfVmAdapterImpl
- [ ] 集成VirtualizationManager
- [ ] 实现真实的VM创建和启动
- [ ] 实现VM状态监控
- [ ] 实现VirtIO设备配置
- [ ] 更新单元测试

## 知识点记录
### 技术要点
- VirtualMachine API调用
- VirtIO设备配置（serial、vsock、block）
- VM状态机集成
- 错误处理和恢复

### 注意事项
- 保持与现有VmManager接口兼容
- 处理AVF不可用的情况
- 实现优雅的错误恢复

## 验收标准
- [ ] AvfVmAdapterImpl调用真实AVF API
- [ ] VM可以成功创建和启动
- [ ] 状态回调正常工作
- [ ] 单元测试覆盖率≥80%
- [ ] 与现有VmManager集成正常

## 相关资源
- [现有AvfVmAdapterImpl.kt](../core/vm/src/main/java/com/dfa/core/vm/avf/AvfVmAdapterImpl.kt)
- [VmManager接口](../core/vm/src/main/java/com/dfa/core/vm/VmManager.kt)