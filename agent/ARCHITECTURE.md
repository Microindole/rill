# 项目架构

## 总体定位

`rill` 当前本质上是一个“数据库内核实验项目”，而不是普通业务系统。

项目核心价值在于：

- 自己实现 SQL 处理链路
- 自己实现底层页式存储与缓存管理
- 自己实现 B+Tree 索引
- 自己实现事务、日志和恢复基础设施
- 提供多种访问入口
- 已形成按产品边界拆分的多模块构建与发布体系

## 当前模块结构

当前仓库已经切到父 `pom` 聚合的多模块结构：

- `rill-core`：数据库内核
- `rill-server`：原生服务端与 MySQL/Navicat 兼容服务端
- `rill-client`：CLI、GUI 与本地工具入口
- `rill-app-web`：Spring Boot Web 壳
- `rill-launcher`：本地开发期统一入口

当前发布边界也已经与模块边界基本对齐：

- 数据库主体安装包：`rill-core + rill-server`，并带上 CLI
- 可选本地组件：GUI、MySQL/Navicat 兼容服务端
- Web 控制台：`rill-app-web` 单独发布，可为纯后端 jar 或内嵌前端静态资源的单文件 jar

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

正式分发时，这些入口已不再通过单体 jar 混合交付，而是按模块与发布物分类收口。

### 1.25 模块边界

当前已经形成最小 `app` 层骨架：

- `app.service.QueryProcessorRegistry`
- `app.service.RillQueryService`
- `app.web.HealthController`
- `app.service.QueryTraceService`
- `app.web.QueryController`

这代表 Spring Boot 不再只是一个启动壳，而是开始成为正式的外层适配层。
当前 `web/` 目录下也已经起出了第一版前端骨架，采用 `Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + Tailwind CSS + Vue Flow`。
当前 `rill-app-web` 也已支持两种打包方式：

- 纯 Spring Boot jar
- 通过 `with-ui` profile 内嵌 `web/dist` 的单文件 jar

### 2. SQL 处理层

主要路径：

- `core.sql.ast`
- `core.sql.lexer`
- `core.sql.parser`
- `core.sql.semantic`
- `core.sql.planner`
- PlanNode 体系

### 3. 执行层

主要路径：

- `core.execution.QueryProcessor`
- `core.execution.ExecutionEngine`
- `core.execution.operator.*`
- `core.execution.operator.TupleIterator`

当前这一层内部也开始出现明确协作者：

- `QueryRuntime` 负责数据库运行时初始化
- `QueryCompiler` 负责 SQL 的 parse / analyze / plan 编译流程
- `BuiltInCommandHandler` 负责 `CRASH_NOW / FLUSH_BUFFER` 这类内建命令
- `StatementTableNameResolver` 负责从 AST 提取协议层需要的表名
- `QueryResultRenderer` 负责结果文本渲染
- `ExecutionSupport` 负责 `TableHeap` 与谓词构造辅助
- `ProjectionColumnResolver` 负责投影列索引解析
- `QueryExecutorBuilder` 负责查询类执行器的递归装配

### 4. 元数据与存储层

主要组件：

- `core.catalog.*`
- `core.model.*`
- `core.exception.*`
- `core.storage.*`
- `core.storage.database.DatabaseManager`

当前这一层内部已经开始继续拆细：

- `Catalog` 负责目录主编排
- `PermissionRegistry` 负责用户/权限缓存与权限匹配
- `CatalogMetadataStore` 负责 `_catalog_tables / _catalog_columns` 的读写细节
- `IndexRegistry` 负责索引元数据的注册、查询和移除
- `UserDirectoryStore` 负责 `_catalog_users / _catalog_privileges` 的读写细节

### 5. 事务与恢复层

主要组件：

- `core.transaction.*`

当前恢复链路已经开始从单一长方法拆成阶段方法：

- Analysis
- Redo
- Undo
- DDL 日志应用
- DML 日志应用

当前这一层内部也开始出现明确协作者：

- `RecoveryManager` 负责恢复阶段编排
- `RecoveryApplier` 负责具体物理 redo / undo 操作

### 5.5 系统层基础设施语义

当前已经明确：`storage + transaction + catalog` 共同组成系统层基础设施。

其中：

- `storage` 负责页、磁盘、缓冲池、索引
- `transaction` 负责锁、事务、日志、恢复
- `catalog` 负责元数据目录及其持久化基础设施

当前 `QueryRuntime` 也已不再直接散落式创建所有基础设施对象，而是通过默认运行时组装器统一初始化。
当前日志与锁管理也已开始通过 `LogService`、`LockService` 暴露默认单机实现，执行链路和事务链路不再直接依赖 `LogManager`、`LockManager`。
当前数据库文件定位也已开始通过 `DatabasePathResolver` 暴露本地默认实现，`QueryRuntime` 可通过 `RuntimeInfrastructureFactory` 接入不同的基础设施组装策略。
当前 `Catalog` 也已开始复用同一套路径解析策略，默认权限重载不再单独写死本地 `default` 库文件路径。
当前 `Catalog` 的默认权限重载逻辑也已下沉到独立协作者 `PermissionReloadAccess`，目录层正在从“大类直接访问磁盘”转向“编排层 + 协作者”结构。
当前 `Catalog` 的元数据读写也已开始通过 `CatalogMetadataAccess` 暴露默认页式实现，目录层不再直接绑定唯一的元数据访问实现。
当前 `Catalog` 的用户目录与索引目录也已开始通过 `UserDirectoryAccess`、`IndexCatalogAccess` 暴露默认实现，目录层协作者边界进一步完整。
当前 `Catalog` 的默认协作者装配也已收口到 `CatalogCollaborators`，目录层开始具备更明确的默认装配入口。
当前页访问能力也已开始通过 `PageAccess` 暴露默认实现，目录层协作者不再要求直接绑定 `BufferPoolManager`。
当前恢复链路也已开始复用 `PageAccess`，系统层正在逐步从“直接依赖缓冲池实现”收口到“页访问接口 + 默认缓冲池实现”结构。
当前 `TableHeap` 与执行支撑层也已开始复用 `PageAccess`，目录/恢复/表堆三条链路的页访问边界已开始统一。

SQL 编译链路的 DML 分支也已经开始继续拆细：

- `Planner` 负责语句级规划分发
- `SelectPlanBuilder`、`InsertPlanBuilder`、`DeletePlanBuilder`、`UpdatePlanBuilder` 负责分语句构建计划
- `SemanticAnalyzer` 负责语句级语义分发
- `SelectSemanticValidator`、`InsertSemanticValidator`、`DeleteSemanticValidator`、`UpdateSemanticValidator` 负责分语句校验
- `SemanticValidationSupport` 负责 DML 单表权限、列存在性、字面量类型兼容等共享校验

定义类与权限类分支也已经继续拆细：

- `CreateTablePlanBuilder`、`CreateIndexPlanBuilder`、`AlterTablePlanBuilder`、`DropTablePlanBuilder` 负责 DDL 计划构建
- `CreateUserPlanBuilder`、`GrantPlanBuilder` 负责权限类计划构建
- `CreateTableSemanticValidator`、`AlterTableSemanticValidator`、`DropTableSemanticValidator` 负责 DDL 语义校验
- `CreateUserSemanticValidator`、`GrantSemanticValidator` 负责权限类语义校验
- `DefinitionValidationSupport` 负责 root 权限要求、列定义数据类型等共享定义校验

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
当前产品分发边界也已经明确：Spring Boot / Web 控制台不进入数据库主体安装包，而是保持独立 jar 发布。
Web UI 的第一阶段定位也已经明确：不是介绍页，而是数据库可视化控制台，重点展示 SQL 执行、结果返回、执行流程与源码映射。
当前已经有第一版正式接口：

- `POST /api/query/execute`
- `GET /api/query/history`
- `GET /api/query/trace/{traceId}`
- `GET /api/health`

当前 `app` 层返回给前端的结果已经以结构化 `rows / columns / traceSteps` 为主，`rawResult` 仅保留为辅助展示，不再作为前端表格数据来源。
当前 trace 也已经开始由运行时实际分发点产出，`SemanticAnalyzer`、`Planner`、`ExecutionEngine` 会记录命中的 validator / builder / executor。

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
当前已完成的另一项结构清理是：`core.common` 已被拆除，基础模型与异常已迁移到 `core.model` 和 `core.exception`。
当前已完成的第三项结构收口是：`core.compiler` 已重命名为 `core.sql`，`core.engine` 与 `core.executor` 已合并为 `core.execution`。
当前已完成的第四项结构收口是：AST 已从 `core.sql.parser.ast` 提升为独立的 `core.sql.ast`。

### 4. 下一阶段会引入更多外部能力

后续规划包含 Web UI、网络编程、Redis、submodule、自动化测试与 CI/CD，因此底层设计必须先稳定，否则会在扩展时迅速失控。

### 5. 发布与安装体系刚完成第一轮收口

当前 CI / release 已能生成：

- Windows 安装包
- Linux/macOS 归档包
- 纯 `rill-app-web` jar
- 带前端静态资源的 `rill-app-web` jar

但这套发布边界还只是第一轮，后续仍需继续补：

- Linux `deb/rpm`
- macOS `.app/.dmg`
- 更严格的 release 产物校验
- 更明确的数据库主体与可选组件安装体验

### 6. Web UI 仍处于第一轮产品化阶段

当前 `web/` 已可独立构建，并已优先调用 Spring Boot 的真实查询接口；当前仍保留 mock 回退以保证前端独立开发。
当前前后端已经按分离方式组织：前端通过环境变量指定 API 基地址，后端通过配置项管理 CORS，而不是在代码里硬编码联调地址。
当前 Web UI 已开始从“SQL 输入 + 结果表”扩成“演示台”结构，会同时展示 SQL、模块边界、能力摘要和扩展路线。

## 重构方向

近期重构目标：

- 以统一入口为中心重新整理运行模式
- 明确哪些入口保留、哪些废弃、哪些迁移为子命令
- 去掉非必要的启动副作用
- 先补齐架构和状态文档，再做大功能改造
- 在底层重构完成后，再推进 Web UI、网络能力和工程化体系
- 以结构化 trace 为核心，把 Web UI 做成数据库执行链路的可观测性界面

