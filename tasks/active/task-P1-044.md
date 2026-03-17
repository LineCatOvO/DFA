# Task-P1-044: DFA项目重命名为CDroid并重新定位

**创建时间**：2026-03-17 08:49:00
**优先级**：P1
**状态**：进行中
**任务锁**：🔒 正在处理 - Planner - 2026-03-17 10:15:00

## 任务描述
将DFA项目重命名为CDroid，并重新定位项目方向：
- 原定位：在Android设备上管理Docker容器（基于AVF虚拟机）
- 新定位：针对docker和其他容器服务如podman的管理面板
- 核心变更：本身不再追求实现docker的直接运行，而是配置docker上下文连接信息
- **CDroid全称**：Container Droid（Container + Droid的简写）

## 用户需求背景

### 原项目问题
- DFA项目已搁置（AVF SDK不对开发者开放）
- 原方案依赖Android AVF虚拟化框架，无法在标准用户环境使用
- 需要系统级权限，普通应用无法使用

### 新定位优势
- 通用性更强：支持多种容器服务（Docker、Podman等）
- 无需虚拟化：通过Docker Context连接远程/本地容器服务
- 跨平台：理论上可支持Android、iOS、Web等多平台
- 降低技术门槛：配置连接信息即可使用

## 执行计划

### Phase 1: 核心文档修改（必须）
- [ ] 步骤1：修改README.md
  - 更新项目名称：DFA → CDroid
  - 更新项目概述：从"Android设备上管理Docker容器"改为"容器服务管理面板"
  - **更新CDroid全称说明**：在项目概述中明确说明"CDroid是Container Droid的简写"
  - 更新技术栈说明
  - 更新项目结构说明
  - 更新使用场景和示例

- [ ] 步骤2：修改应用配置
  - 修改app/src/main/res/values/strings.xml中的app_name为"CDroid"
  - 修改gradle.properties中的项目配置
  - 修改settings.gradle.kts中的rootProject.name为"CDroid"
  - 修改AndroidManifest.xml中的应用配置

- [ ] 步骤3：修改架构文档
  - 更新docs/ARCHITECTURE.md：从AVF架构改为Docker Context架构
  - 更新docs/DOCKER-INTEGRATION.md：从虚拟机集成改为上下文连接
  - 更新docs/INSTALLATION.md：从虚拟机安装改为连接配置
  - 更新docs/FAQ.md：
    - **更新CDroid全称说明**：在Q1中明确说明"CDroid (Container Droid) 是Container Droid的简写"
    - 更新常见问题和解答

- [ ] 步骤4：更新项目文档
  - 更新docs/ROADMAP.md：调整功能规划和里程碑
  - 更新CHANGELOG.md：添加版本变更记录
  - 更新或删除SUSPENDED.md：根据新定位决定

### Phase 2: core模块包名更新（必须）
- [ ] 步骤5：更新core/vm模块包名和目录结构
  - 修改core/vm/build.gradle.kts：namespace从"com.dfa.core.vm"改为"com.cdroid.core.vm"
  - 重命名目录：core/vm/src/main/java/com/dfa/core/vm → core/vm/src/main/java/com/cdroid/core/vm
  - 重命名目录：core/vm/src/test/java/com/dfa/core/vm → core/vm/src/test/java/com/cdroid/core/vm
  - 重命名目录：core/vm/src/androidTest/java/com/dfa/core/vm → core/vm/src/androidTest/java/com/cdroid/core/vm
  - 批量更新所有源代码文件的package声明：package com.dfa.core.vm → package com.cdroid.core.vm
  - 批量更新所有import语句：import com.dfa.core.vm.* → import com.cdroid.core.vm.*

- [ ] 步骤6：更新core/docker模块包名和目录结构
  - 修改core/docker/build.gradle.kts：namespace从"com.dfa.core.docker"改为"com.cdroid.core.docker"
  - 重命名目录：core/docker/src/main/java/com/dfa/core/docker → core/docker/src/main/java/com/cdroid/core/docker
  - 重命名目录：core/docker/src/test/java/com/dfa/core/docker → core/docker/src/test/java/com/cdroid/core/docker
  - 重命名目录：core/docker/src/androidTest/java/com/dfa/core/docker → core/docker/src/androidTest/java/com/cdroid/core/docker
  - 批量更新所有源代码文件的package声明：package com.dfa.core.docker → package com.cdroid.core.docker
  - 批量更新所有import语句：import com.dfa.core.docker.* → import com.cdroid.core.docker.*

- [ ] 步骤7：更新core/common模块包名和目录结构
  - 修改core/common/build.gradle.kts：namespace从"com.dfa.core.common"改为"com.cdroid.core.common"
  - 重命名目录：core/common/src/main/java/com/dfa/core/common → core/common/src/main/java/com/cdroid/core/common
  - 重命名目录：core/common/src/test/java/com/dfa/core/common → core/common/src/test/java/com/cdroid/core/common
  - 批量更新所有源代码文件的package声明：package com.dfa.core.common → package com.cdroid.core.common
  - 批量更新所有import语句：import com.dfa.core.common.* → import com.cdroid.core.common.*

- [ ] 步骤8：更新app模块对core模块的引用
  - 验证app/build.gradle.kts中的依赖声明（已更新为com.cdroid）
  - 更新app模块中所有import语句：import com.dfa.core.* → import com.cdroid.core.*
  - 验证app模块可以正常编译

### Phase 3: 代码调整（按需）
- [ ] 步骤9：调整核心功能模块
  - 移除或重构AVF相关模块（core/vm）
  - 新增Docker Context管理模块
  - 新增容器服务连接模块
  - 新增Podman支持模块

### Phase 4: 测试和验证
- [ ] 步骤10：更新测试用例
  - 更新单元测试
  - 更新集成测试
  - 更新E2E测试

- [ ] 步骤11：验证修改
  - 验证应用名称变更
  - 验证文档一致性
  - 验证代码编译
  - **验证core模块包名更新**：确认所有core模块文件已更新为com.cdroid包名
  - **验证app模块依赖**：确认app模块可以正常引用core模块

## 验收标准

### 必须满足
- [ ] README.md已更新，项目名称为CDroid
- [ ] **README.md已更新CDroid全称说明**：明确说明"CDroid是Container Droid的简写"
- [ ] 应用显示名称为CDroid
- [ ] 项目配置文件已更新（gradle.properties, settings.gradle.kts）
- [ ] 所有文档中的DFA引用已更新为CDroid
- [ ] **FAQ.md已更新CDroid全称说明**：在Q1中明确说明"CDroid (Container Droid) 是Container Droid的简写"
- [ ] 架构文档已更新，反映新的定位
- [ ] CHANGELOG.md已添加变更记录
- [ ] 代码可以正常编译
- [ ] **core/vm模块包名已更新**：namespace为com.cdroid.core.vm，所有源代码文件在com/cdroid目录下
- [ ] **core/docker模块包名已更新**：namespace为com.cdroid.core.docker，所有源代码文件在com/cdroid目录下
- [ ] **core/common模块包名已更新**：namespace为com.cdroid.core.common，所有源代码文件在com/cdroid目录下
- [ ] **core模块所有import语句已更新**：从com.dfa.core.*改为com.cdroid.core.*
- [ ] **app模块可以正常引用core模块**：编译无错误，依赖关系正确

### 建议满足
- [ ] AVF相关模块已移除或重构
- [ ] Docker Context管理模块已实现
- [ ] Podman支持已实现
- [ ] 测试用例已更新并通过

## 风险评估

| 风险描述 | 影响级别 | 可能性 | 应对策略 |
|----------|----------|--------|----------|
| core模块包名变更导致大量代码修改 | 高 | 高 | 使用脚本批量替换，分模块逐步验证 |
| core模块包名变更影响app模块依赖 | 高 | 高 | 先更新core模块，再验证app模块引用 |
| 文档遗漏更新 | 中 | 中 | 使用全局搜索确保所有引用已更新 |
| 架构变更导致现有代码不可用 | 高 | 中 | 保留现有代码，新增新功能模块 |
| 用户困惑（项目定位变更） | 中 | 高 | 在README和CHANGELOG中清晰说明变更原因 |
| CDroid全称说明遗漏 | 低 | 中 | 在README和FAQ中明确说明全称 |
| core模块目录结构变更导致文件丢失 | 高 | 低 | 使用git mv命令重命名，确保文件历史保留 |
| core模块测试文件包名更新遗漏 | 中 | 中 | 同时更新源代码和测试文件的包名

## 需要搜索和分析的内容

### 已完成搜索
- ✅ DFA项目核心文档位置
- ✅ 当前项目结构
- ✅ 现有任务文档
- ✅ 项目配置文件

### 需要进一步分析
- ⏳ 评估包名变更的必要性
- ⏳ 分析AVF模块与新架构的兼容性
- ⏳ 研究Docker Context API和实现方式
- ⏳ 研究Podman API和集成方式

## 相关文件清单

### 核心文档
- /workspaces/AgentWorkspace/projects/DFA/README.md
- /workspaces/AgentWorkspace/projects/DFA/CHANGELOG.md
- /workspaces/AgentWorkspace/projects/DFA/SUSPENDED.md

### 应用配置
- /workspaces/AgentWorkspace/projects/DFA/app/src/main/res/values/strings.xml
- /workspaces/AgentWorkspace/projects/DFA/gradle.properties
- /workspaces/AgentWorkspace/projects/DFA/settings.gradle.kts
- /workspaces/AgentWorkspace/projects/DFA/app/src/main/AndroidManifest.xml

### 架构文档
- /workspaces/AgentWorkspace/projects/DFA/docs/ARCHITECTURE.md
- /workspaces/AgentWorkspace/projects/DFA/docs/DOCKER-INTEGRATION.md
- /workspaces/AgentWorkspace/projects/DFA/docs/INSTALLATION.md
- /workspaces/AgentWorkspace/projects/DFA/docs/FAQ.md
- /workspaces/AgentWorkspace/projects/DFA/docs/ROADMAP.md

### 其他文档
- /workspaces/AgentWorkspace/projects/DFA/docs/SECURITY.md
- /workspaces/AgentWorkspace/projects/DFA/docs/PERFORMANCE.md
- /workspaces/AgentWorkspace/projects/DFA/docs/DEVICE-SUPPORT.md
- /workspaces/AgentWorkspace/projects/DFA/docs/DEVELOPMENT.md
- /workspaces/AgentWorkspace/projects/DFA/docs/API-REFERENCE.md
- /workspaces/AgentWorkspace/projects/DFA/docs/TROUBLESHOOTING.md

## 执行记录
| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-17 08:49 | 创建任务 | 用户提出DFA项目重命名和重新定位需求 |
| 2026-03-17 08:50 | Planner分析 | 完成深度搜索和分析，制定详细执行计划 |
