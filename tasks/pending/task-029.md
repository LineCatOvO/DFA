# Task-029: 修复core:vm模块剩余编译错误

**创建时间**：2026-03-16
**优先级**：P0（阻塞编译）
**状态**：待处理
**MVP阶段**：阶段0 - 环境验证
**依赖**：task-028
**完成时间**：

## 任务描述
修复core:vm模块中剩余的编译错误，确保项目可以成功编译。

## 待修复错误

### 1. VmResources.kt - 属性引用错误和VmFeature重复定义
```
e: Unresolved reference 'availableDiskSpaceGb'
e: Unresolved reference 'gpuAvailable'
e: Redeclaration: enum class VmFeature
```
**修复方案**：检查VmResources数据类定义，修复属性引用，删除重复的VmFeature枚举

### 2. SshChannelImpl.kt - JSch API错误
```
e: Unresolved reference 'setPortForwardingD'
e: Argument type mismatch for getFingerPrint
```
**修复方案**：使用正确的JSch API方法

### 3. SshConfig.kt - validate方法无法覆盖
```
e: 'validate' in 'ChannelConfig' is final and cannot be overridden
```
**修复方案**：重命名validate方法或修改父类

### 4. VmModule.kt - 依赖注入配置错误
```
e: Interface 'SocketChannelFactory' does not have constructors
e: No parameter with name 'monitor' found
```
**修复方案**：修复Hilt依赖注入配置

### 5. QemuMonitorImpl.kt - isConnected冲突和JSON解析错误
```
e: Conflicting declarations: val isConnected: Boolean
e: Cannot infer type for this parameter
```
**修复方案**：修复isConnected属性定义，修复JSON解析调用

### 6. QemuVmAdapterImpl.kt - getSupportedFeatures返回类型
```
e: Return type of 'getSupportedFeatures' is not a subtype
```
**修复方案**：检查并修复返回类型

## 执行计划
- [ ] 修复VmResources.kt错误
- [ ] 修复SshChannelImpl.kt错误
- [ ] 修复SshConfig.kt错误
- [ ] 修复VmModule.kt错误
- [ ] 修复QemuMonitorImpl.kt错误
- [ ] 修复QemuVmAdapterImpl.kt错误
- [ ] 验证编译通过

## 验收标准
- [ ] 项目编译通过（./gradlew assembleDebug）
- [ ] 无编译错误