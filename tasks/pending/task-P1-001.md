# Task-P1-001: Docker模块辅助单元测试创建

**创建时间**：2026-03-16
**优先级**：P1
**状态**：待处理
**依赖**：task-P0-001

## 任务描述
为Docker模块创建辅助单元测试，覆盖DockerConfig、DockerException等辅助类。

## 执行计划
- [ ] 步骤1：创建DockerConfigTest.kt
  - 默认值验证测试
  - Builder模式测试
  - 配置验证测试
- [ ] 步骤2：创建DockerExceptionTest.kt
  - 异常类型测试
  - 错误码映射测试
  - 消息格式测试
- [ ] 步骤3：运行测试验证

## 验收标准
- [ ] 新增测试文件至少2个
- [ ] 测试覆盖辅助功能
- [ ] 所有新增测试通过

## 相关文件
- `core/docker/src/main/java/com/dfa/core/docker/DockerConfig.kt`
- `core/docker/src/main/java/com/dfa/core/docker/DockerException.kt`

## 执行记录
| 时间 | 操作 | 说明 |
|------|------|------|