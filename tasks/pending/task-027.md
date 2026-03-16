# Task-027: 修复VmEvent编译错误

**创建时间**：2026-03-16
**优先级**：P0（阻塞编译）
**状态**：进行中
**MVP阶段**：阶段0 - 环境验证
**依赖**：无
**完成时间**：

## 任务描述
修复项目编译错误，创建缺失的类型定义并修复类型不匹配问题。

## 背景
项目架构重构（从AVF迁移到Termux+QEMU）时，多个类型定义缺失或类型不匹配，导致编译失败。

## 已完成
- [x] 创建VmCallback接口
- [x] 创建VmFeature枚举
- [x] 创建QemuVmCallback接口
- [x] 添加QemuVmHandle类型别名

## 待修复错误

### 1. QemuMonitorImpl.kt - Serializable冲突
```
e: Overload resolution ambiguity between candidates:
annotation class Serializable : Annotation
interface Serializable : Any
```
**修复方案**：使用完整限定名 `kotlinx.serialization.Serializable`

### 2. QemuProcess.kt - pid引用错误
```
e: Unresolved reference 'pid'
```
**修复方案**：检查QemuProcess类定义，添加或修复pid属性

### 3. QemuProcessManagerImpl.kt - workingDirectory和pid引用错误
```
e: Unresolved reference 'workingDirectory'
e: Unresolved reference 'pid'
```
**修复方案**：检查并修复相关属性引用

### 4. QemuVmAdapterImpl.kt - 类型不匹配
```
e: Argument type mismatch: actual type is 'AvfVmHandle', but 'VmHandle?' was expected
e: Return type of 'getSupportedFeatures' is not a subtype
e: Argument type mismatch: actual type is 'kotlin.String', but 'VmState' was expected
```
**修复方案**：
- 将AvfVmHandle转换为VmHandle
- 修复getSupportedFeatures返回类型
- 修复回调方法调用

### 5. TermuxPackageManagerImpl.kt - 参数错误
```
e: No value passed for parameter 'version'
e: Suspend function should be called only from a coroutine
```
**修复方案**：添加缺失参数，修复suspend函数调用

## 执行计划
- [ ] 修复QemuMonitorImpl.kt的Serializable冲突
- [ ] 修复QemuProcess.kt的pid引用
- [ ] 修复QemuProcessManagerImpl.kt的引用错误
- [ ] 修复QemuVmAdapterImpl.kt的类型不匹配
- [ ] 修复TermuxPackageManagerImpl.kt的参数错误
- [ ] 验证编译通过

## 验收标准
- [ ] 项目编译通过（./gradlew assembleDebug）
- [ ] 无编译错误

## 相关资源
- [VmCallback.kt](../core/vm/src/main/java/com/dfa/core/vm/VmCallback.kt)
- [QemuVmCallback.kt](../core/vm/src/main/java/com/dfa/core/vm/qemu/QemuVmCallback.kt)