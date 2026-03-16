# Task-027: 修复VmEvent编译错误

**创建时间**：2026-03-16
**优先级**：P0（阻塞编译）
**状态**：待处理
**MVP阶段**：阶段0 - 环境验证
**依赖**：无
**完成时间**：

## 任务描述
修复项目编译错误，创建缺失的VmEvent sealed class。

## 背景
项目架构重构（从AVF迁移到Termux+QEMU）时，VmEvent sealed class未正确创建，导致VmManagerImpl和VmStateMachine无法编译。

## 错误信息
```
e: [ksp] InjectProcessingStep was unable to process 'VmManagerImpl(...)' 
because 'error.NonExistentClass' could not be resolved.
```

## 执行计划
- [ ] 分析VmManagerImpl和VmStateMachine对VmEvent的使用
- [ ] 创建VmEvent.kt文件
- [ ] 定义VmEvent sealed class及其子类
- [ ] 验证编译通过

## 知识点记录
### 技术要点
- VmEvent是虚拟机状态机的事件类型
- 使用sealed class实现类型安全的事件处理
- 事件类型：Start, Stop, Pause, Resume, Reset, Migrate, Error

### 注意事项
- 确保与VmStateMachine的兼容性
- 确保与VmManagerImpl的兼容性

## 验收标准
- [ ] VmEvent.kt文件创建完成
- [ ] 项目编译通过（./gradlew assembleDebug）
- [ ] 无编译错误

## 相关资源
- [VmManagerImpl.kt](../core/vm/src/main/java/com/dfa/core/vm/VmManagerImpl.kt)
- [VmStateMachine.kt](../core/vm/src/main/java/com/dfa/core/vm/statemachine/VmStateMachine.kt)