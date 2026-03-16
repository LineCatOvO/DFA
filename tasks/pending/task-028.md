# Task-028: 修复QEMU模块剩余编译错误

**创建时间**：2026-03-16
**优先级**：P0（阻塞编译）
**状态**：待处理
**MVP阶段**：阶段0 - 环境验证
**依赖**：task-027
**完成时间**：

## 任务描述
修复QEMU模块中剩余的编译错误，确保项目可以成功编译。

## 待修复错误

### 1. QemuMonitorImpl.kt - isConnected属性冲突
```
e: Overload resolution ambiguity between candidates:
val isConnected: AtomicBoolean
val isConnected: Boolean
```
**修复方案**：重命名其中一个属性，避免命名冲突

### 2. QemuMonitorImpl.kt - Serializable冲突
```
e: Overload resolution ambiguity between candidates:
annotation class Serializable : Annotation
interface Serializable : Any
```
**修复方案**：使用完整限定名 `kotlinx.serialization.Serializable`

### 3. QemuProcess.kt:280 - pid引用错误
```
e: Unresolved reference 'pid'
```
**修复方案**：检查Process.pid()方法可用性，使用兼容方式获取PID

### 4. QemuProcessManagerImpl.kt:177 - pid引用错误
```
e: Unresolved reference 'pid'
```
**修复方案**：同上，使用兼容方式获取PID

### 5. QemuVmAdapterImpl.kt:513 - getSupportedFeatures返回类型
```
e: Return type of 'getSupportedFeatures' is not a subtype
```
**修复方案**：检查返回类型是否匹配VmFeature

### 6. QemuVmAdapterImpl.kt:958 - isKvmAvailable调用
```
e: Suspend function should be called only from a coroutine
```
**修复方案**：将调用移至suspend上下文或使用runBlocking

## 执行计划
- [ ] 修复QemuMonitorImpl.kt的isConnected属性冲突
- [ ] 修复QemuMonitorImpl.kt的Serializable冲突
- [ ] 修复QemuProcess.kt的pid引用
- [ ] 修复QemuProcessManagerImpl.kt的pid引用
- [ ] 修复QemuVmAdapterImpl.kt的getSupportedFeatures返回类型
- [ ] 修复QemuVmAdapterImpl.kt的isKvmAvailable调用
- [ ] 验证编译通过

## 验收标准
- [ ] 项目编译通过（./gradlew assembleDebug）
- [ ] 无编译错误