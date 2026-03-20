# 项目架构

## 总体定位

`rill` 当前本质上是一个“数据库内核实验项目”，而不是普通业务系统。

项目核心价值在于：

- 自己实现 SQL 处理链路
- 自己实现底层页式存储与缓存管理
- 自己实现 B+Tree 索引
- 自己实现事务、日志和恢复基础设施
- 提供多种访问入口

## 当前分层

### 1. 入口层

主要入口包括：

- 统一入口：`com.indolyn.rill.app.boot.RillLauncher`
- Spring Boot 入口：`com.indolyn.rill.app.boot.RillApplication`
- 原生服务端：`com.indolyn.rill.access.protocol.ServerHost`
- MySQL 协议服务端：`com.indolyn.rill.access.protocol.ServerRemote`
- 终端客户端：`com.indolyn.rill.access.cli.InteractiveShell`
- GUI 客户端：`com.indolyn.rill.access.gui.AdvancedShell`
- 工具入口：`com.indolyn.rill.tools.DataReader`、`com.indolyn.rill.tools.LogReader`

### 1.5 Spring Boot 应用层

当前已经形成最小 `app` 层骨架：

- `app.service.QueryProcessorRegistry`
- `app.service.RillQueryService`
- `app.web.HealthController`

这代表 Spring Boot 不再只是一个启动壳，而是开始成为正式的外层适配层。

### 2. SQL 编译层

主要路径：

- `core.compiler.lexer`
- `core.compiler.parser`
- `core.compiler.semantic`
- `core.compiler.planner`
- PlanNode 体系

### 3. 执行层

主要路径：

- `core.engine.QueryProcessor`
- `core.engine.ExecutionEngine`
- `core.executor.*`
- `core.executor.TupleIterator`

### 4. 元数据与存储层

主要组件：

- `core.catalog.*`
- `core.storage.*`
- `core.DatabaseManager`

### 5. 事务与恢复层

主要组件：

- `core.transaction.*`

### 6. 客户端与工具层

主要组件：

- access.cli.InteractiveShell
- access.gui.AdvancedShell
- access.protocol.ServerHost
- access.protocol.ServerRemote
- tools.DataReader
- tools.LogReader

## 目标架构方向

后续入口层要逐步收敛为四个主要外部访问方向：

1. CLI
2. GUI
3. Web UI
4. Navicat 兼容入口

其中：

- CLI 用于终端操作、开发调试、脚本化调用
- GUI 用于本地桌面交互
- Web UI 用于部署后的展示和演示
- Navicat 兼容入口用于外部数据库客户端接入

Spring Boot 在后续应主要承担 Web UI 和服务化承载角色，而不是继续保留演示型启动副作用。
当前已完成的修正是：Spring Boot 启动不再自动触发 `ShellRunner` 演示逻辑。

## 当前主执行链路

典型 SQL 执行路径：

1. 客户端或服务端接收 SQL
2. `QueryProcessor` 负责组装处理流程
3. `Lexer` 分词
4. `Parser` 构造 AST
5. `SemanticAnalyzer` 做语义和权限检查
6. `Planner` 生成计划节点
7. `ExecutionEngine` 构造执行器树
8. 执行器访问存储、索引、事务和目录元数据
9. 返回格式化结果

## 当前主要问题

### 1. 入口过多

项目目前存在多个 `main` 入口，职责边界不清，部分入口只适合实验，不适合长期保留。

### 2. Spring Boot 入口角色刚完成第一次纠偏

此前 Spring Boot 启动时会自动执行演示/测试逻辑。该问题已经被修正，但 Spring Boot 的正式职责边界仍需要继续明确为 Web UI 和服务化承载层。

### 3. UI、工具、内核耦合偏重

数据库内核、客户端界面、辅助工具目前仍在一个工程中共存，但已经完成两步关键拆分：

- 入口层拆入 `app / access / tools`
- 数据库内核主包拆入 `core`

后续需要继续强化边界，而不是继续混放。

### 4. `core` 边界刚建立，还需要继续净化

虽然数据库内核已经迁入 `com.indolyn.rill.core.*`，但这只是第一阶段。后续仍需继续检查：

- `core` 是否还依赖外层入口类型
- access / tools 是否直接承担了过多内核职责
- Spring Boot 适配层是否真正只做外层承载

当前已完成的一步是：会话抽象已下沉到 `core.session`，`core` 不再直接依赖 `access.protocol.Session`。

### 4. 下一阶段会引入更多外部能力

后续规划包含 Web UI、网络编程、Redis、submodule、自动化测试与 CI/CD，因此底层设计必须先稳定，否则会在扩展时迅速失控。

## 重构方向

近期重构目标：

- 以统一入口为中心重新整理运行模式
- 明确哪些入口保留、哪些废弃、哪些迁移为子命令
- 去掉非必要的启动副作用
- 先补齐架构和状态文档，再做大功能改造
- 在底层重构完成后，再推进 Web UI、网络能力和工程化体系
