# Task-003: VM镜像构建系统

**创建时间**：2026-03-14
**优先级**：高
**状态**：待处理
**阶段**：Phase 1 - 基础架构
**预估工期**：2周

## 任务描述
构建支持Docker运行的Microdroid虚拟机镜像，配置内核和必要的系统组件。

## 执行计划
- [ ] 研究Microdroid镜像结构
- [ ] 配置内核（KVM、VirtIO、Namespaces、Cgroups）
- [ ] 创建镜像构建脚本
- [ ] 实现镜像打包和签名
- [ ] 测试镜像启动

## 技术要点
- 内核配置：CONFIG_KVM, CONFIG_NAMESPACES, CONFIG_CGROUPS
- VirtIO驱动配置
- 镜像格式：Android VM Image (AVB签名)
- 存储驱动：overlay2

## 验收标准
- [ ] VM镜像可正常启动
- [ ] 内核配置满足Docker运行要求
- [ ] 镜像大小 < 100MB
- [ ] 构建过程可重复

## 依赖关系
- 依赖：Task-002（AVF虚拟机管理模块）

## 相关资源
- Microdroid源码：https://android.googlesource.com/platform/packages/modules/Virtualization/
- 内核配置参考：https://github.com/ExTV/avf-android