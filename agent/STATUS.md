# 当前状态

## 当前阶段

阶段名称：内核第一轮重构已基本完成，当前重心转向编译器可扩展性与 PostgreSQL 方言收口

## 已完成

- 初步梳理了项目整体结构和技术栈
- 确认项目使用 Java 21
- 确认并测试了多个现有入口的可启动性
- 增加了统一入口 `RillLauncher`
- 增加了跨平台脚本：`scripts/build.cmd`、`scripts/build.sh`、`scripts/rill.cmd`、`scripts/rill.sh`
- 建立了 `agent` 文档体系第一版
- 明确了项目后续的大流程规划
- 明确了运行入口的目标四分类：CLI / GUI / Web UI / Navicat 兼容
- 补充了运行模块的入口保留与收敛策略
- 修正了 `.gitignore` 中对 Maven Wrapper 的错误忽略规则
- 清理了 Spring Boot 启动副作用，保留了 Spring Boot 但移除了自动 demo 执行
- 完成了第一阶段目录迁移：入口层、Spring Boot 层、工具层开始按职责拆包
- 明确当前策略为“先拆目录，不立刻加多个 Maven 模块”
- 完成了 `core` 第一阶段迁移：数据库内核主包收敛到 `com.indolyn.rill.core.*`
- 清理了 `core` 对 `access` 的直接反向依赖，会话抽象已下沉到 `core.session`
- 建立了 `app` 最小骨架：service registry、query service、health controller
- 增加了 `.gitattributes`，统一跨平台换行策略
- 增加了 `.editorconfig`，统一基础缩进与换行规则
- 建立了 IntelliJ IDEA Java 代码风格基准文件
- 已按 IntelliJ IDEA 项目级代码风格对整仓 Java 文件完成一次重格式化
- 完成数据库内核第二批职责下沉：`SELECT` 计划构建、`SELECT` 语义检查、谓词构建已拆出独立组件
- 完成 `core` 目录第一批结构清理：移除 `core.common`，拆分为 `core.model` 与 `core.exception`
- 完成 `core` 主模块命名重构：`compiler -> sql`，`engine/executor -> execution`
- 完成 `core.sql.ast` 提升：AST 已从 `sql.parser.ast` 提升为独立中间层
- 完成 `DatabaseManager` 下沉：从 `core` 根级移动到 `core.storage.database`
- 完成 `Catalog` 第一轮瘦身：权限缓存与匹配逻辑已下沉到 `PermissionRegistry`
- 完成 `Catalog` 第二轮瘦身：系统表读写细节已下沉到 `CatalogMetadataStore`
- 完成 `Catalog` 第三轮瘦身：索引元数据注册与查询已下沉到 `IndexRegistry`
- 完成 `Catalog` 第四轮瘦身：用户目录与权限目录页操作已下沉到 `UserDirectoryStore`
- 完成 `RecoveryManager` 第一轮瘦身：恢复流程已拆分为 analysis / redo / undo / DML 应用方法
- 完成 `RecoveryManager` 第二轮瘦身：DDL 恢复分支已下沉为独立方法
- 完成 `RecoveryManager` 第三轮瘦身：物理 redo / undo 操作已下沉到 `RecoveryApplier`
- 完成 `QueryProcessor` 第一轮瘦身：运行时初始化已下沉到 `QueryRuntime`，结果渲染已下沉到 `QueryResultRenderer`
- 完成 `ExecutionEngine` 第一轮瘦身：重复装配细节已下沉到 `ExecutionSupport` 和 `ProjectionColumnResolver`
- 完成数据库内核第一批核心重构：`Planner`、`SemanticAnalyzer`、`ExecutionEngine` 已从巨型 `instanceof` 分发改为注册式处理结构
- 完成编译链路第二轮职责下沉：`INSERT / UPDATE / DELETE` 的计划构建已拆为独立 builder，DML 语义校验已拆为独立 validator，并引入 `SemanticValidationSupport` 统一单表校验支持
- 完成编译链路第三轮职责下沉：`CREATE / ALTER / DROP / CREATE INDEX / CREATE USER / GRANT` 的计划构建与语义校验已继续拆为独立 builder / validator，并引入 `DefinitionValidationSupport`
- 完成编译链路第四轮收口：`Planner` 与 `SemanticAnalyzer` 中的桥接方法已基本移除，`ShowTables` 也已纳入统一 validator 注册结构
- 完成执行链路第二轮职责下沉：`QueryProcessor` 已拆出 `QueryCompiler / BuiltInCommandHandler / StatementTableNameResolver`，`ExecutionEngine` 已拆出 `QueryExecutorBuilder`
- 完成测试目录第一轮收口：测试已按 `execution / sql / storage / transaction / access.protocol` 主结构迁移，旧的 `dcl / ddl / improve / index / Protocol / replacement` 测试目录已清理
- 开始补执行链路回归测试：新增 `QueryCompilerTest` 与 `QueryProcessorTest`，覆盖新拆出的编译协作者和内建命令/协议归一化行为
- 完成编译链路测试第一轮现代化：`PlannerTest` 与 `SemanticAnalyzerTest` 已改为 JUnit 5，并收口到当前计划/语义行为断言
- 完成执行引擎测试第一轮补强：新增 `ExecutionEngineTest`，覆盖命令节点注册、查询节点注册以及不支持节点报错，并确认查询计划执行需要非空事务
- 完成目录与恢复测试第一轮补强：新增 `CatalogTest`，修复 `RecoveryTest`，并补出恢复阶段锁释放和 `UPDATE` undo 的实现缺口
- 完成 Web UI 第一轮启动：在 `web/` 下建立 `Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + Tailwind CSS + Vue Flow` 前端骨架，并完成首次构建验证
- 完成 Web API 第一轮打通：新增 `query / trace / history` 接口、CORS 配置、结构化 trace DTO，并让 `web/` 前端优先调用真实后端接口
- 完成前后端分离第一轮收口：前端 API 地址改为环境变量配置，后端 CORS 改为配置项，`web/` 不再写死依赖 `localhost:8080`
- 完成结构化结果下沉：`QueryProcessor` 已支持结构化结果执行，`app` 层不再解析文本表格来生成 rows/columns
- 完成真实 trace 埋点第一轮：`SemanticAnalyzer`、`Planner`、`ExecutionEngine` 已在实际分发点记录命中的 validator / builder / executor 组件
- 完成系统层第一轮语义收口：新增系统层文档，并将 `QueryRuntime` 的基础设施初始化抽到默认组装器，减少运行时直接写死依赖
- 完成系统层第二轮最小接口抽象：新增 `LogService`、`LockService`，并将事务/恢复/执行链路切到接口依赖
- 完成系统层第三轮可替换边界抽象：新增 `RuntimeInfrastructureFactory`、`DatabasePathResolver`，并为本地单机运行保留默认实现
- 完成系统层第四轮收口：`Catalog` 已改为依赖 `DatabasePathResolver`，默认权限重载不再直接写死本地数据库路径
- 完成系统层第五轮收口：`Catalog` 的默认权限重载已下沉到独立协作者 `PermissionReloadAccess`
- 完成系统层第六轮收口：`Catalog` 的元数据读写已开始依赖 `CatalogMetadataAccess` 接口
- 完成系统层第七轮收口：用户目录与索引目录已分别开始依赖 `UserDirectoryAccess`、`IndexCatalogAccess`
- 完成系统层第八轮收口：新增 `CatalogCollaborators`，`Catalog` 的默认协作者装配入口已显式化
- 完成系统层第九轮收口：新增 `PageAccess`，目录层已开始从 `BufferPoolManager` 直接依赖切到页访问接口
- 完成系统层第十轮收口：恢复链路已开始从 `BufferPoolManager` 直接依赖切到 `PageAccess`
- 完成系统层第十一轮收口：`TableHeap` 与执行支撑层已切到 `PageAccess`，恢复链路中的缓冲池桥接已移除
- 完成统一构建入口修复：`mvnw.cmd / mvnw` 与 `scripts/build.* / scripts/rill.*` 已统一优先使用 `JAVA21_HOME`
- 完成编译器第五轮收口：`Lexer` 已引入关键字注册表，`Parser` 已引入注册式语句分发，DDL 类型解析已切到 `TypeReferenceNode + PostgreSqlTypeResolver`
- 完成类型系统第一轮物理扩展：新增 `SMALLINT / BIGINT / TIMESTAMP` 内部物理类型，并打通值模型、序列化、INSERT/UPDATE 字面量转换与 PostgreSQL 类型解析
- 完成类型系统第二轮语义收口：补齐 `BOOLEAN / DATE / VARCHAR(n) / CHAR(n)` 回归测试，并为 `VARCHAR / CHAR / NUMERIC` 增加参数合法性校验
- 完成类型系统第三轮别名边界收口：补齐 `REAL / FLOAT8 / NUMERIC / TEXT` 的别名回归，并修正 `TEXT` 不应接受长度参数的问题
- 完成字符串长度约束第一轮落地：`Column` 已保留类型声明与参数，`VARCHAR/CHAR` 长度约束已落到 schema、catalog 持久化、语义检查与 `TableHeap` 写入校验
- 完成协议层类型展示收口：`SHOW CREATE TABLE`、`SHOW FULL COLUMNS` 与协议类型描述已开始复用真实列声明，不再一律退回默认 `VARCHAR(255)`
- 完成 `NUMERIC(p,s)` 第一轮真实约束落地：精度/小数位限制已下沉到 `Column`、语义检查与 `TableHeap` 写入校验，并补齐重启后回归
- 完成 `NUMERIC(p,s)` 的 `UPDATE` 重启回归补强，并让协议层 `SHOW CREATE TABLE` 输出独立主键定义，`SHOW FULL COLUMNS` 返回 `PRI/NO`
- 完成 DDL 列约束元数据第一轮贯通：`NOT NULL / DEFAULT / PRIMARY KEY` 已从 parser/AST 进入 planner、`Column`、catalog 持久化与重启恢复
- 完成协议层 DDL 展示补齐：`SHOW CREATE TABLE` 已输出 `NOT NULL / DEFAULT / PRIMARY KEY / KEY`，`SHOW FULL COLUMNS` 与 `information_schema.columns` 也开始复用同一份空值、默认值和索引元数据

## 已确认事实

- 项目不是普通 CRUD，而是数据库内核实验项目
- 当前存在多个入口，后续需要收敛
- `ServerHost` 与 `ServerRemote` 都能启动
- `InteractiveShell` 能启动
- `AdvancedShell` 与 `LogReader` 属于 GUI 型入口
- `DataReader` 是交互式工具入口
- `RillApplication` 当前不适合直接作为正式启动入口
- `RillApplication` 现已不再自动执行 demo 逻辑
- `ShellRunner` 现为手工启动的演示工具，不再污染 Spring Boot 启动
- 当前已完成第一阶段包迁移：`app.boot`、`access.*`、`tools`
- 当前已完成数据库内核主包迁移：`catalog/common/compiler/engine/executor/storage/transaction/DatabaseManager` 已进入 `core`
- 当前 `core` 已不再直接依赖 `access`、`tools`、`app`
- 当前 `app` 已开始作为 Spring Boot 外层适配层调用 `core`
- 当前已开始统一仓库换行策略，减少 Windows 环境下的提交噪音
- 当前 Java 代码风格基准改为 IntelliJ IDEA 默认风格变体：4 空格缩进、`{` 行尾
- 当前主代码与测试代码都已按新的 IDEA 风格统一过一轮
- 当前编译器与执行器主分发点已开始遵守开闭原则，新增语句/计划类型不再必须改动单一超长方法
- 当前 `Planner`、`SemanticAnalyzer`、`ExecutionEngine` 已进一步退化为编排层，核心细节开始下沉
- 当前 `core` 顶层目录已收敛为 `catalog / exception / execution / model / session / sql / storage / transaction`
- 当前 `core.sql` 主链路已明确为 `lexer / parser / ast / semantic / planner`
- 当前 `catalog` 与 `transaction` 已开始从巨型流程类转向协作者 + 阶段方法结构
- 当前 `Catalog` 已形成 `Catalog + PermissionRegistry + CatalogMetadataStore + IndexRegistry + UserDirectoryStore` 的初步协作结构
- 当前 `transaction` 已形成 `RecoveryManager + RecoveryApplier` 的恢复协作结构
- 当前恢复流程已修复假事务持锁不释放的问题，redo/undo 不再在恢复阶段自阻塞
- 当前恢复流程已修复 `UPDATE` 撤销不完整的问题，恢复时会恢复旧槽位并删除补写的新 tuple
- 当前 `execution` 已形成 `QueryProcessor + QueryRuntime + QueryResultRenderer + ExecutionEngine + ExecutionSupport + ProjectionColumnResolver` 的初步协作结构
- 当前 `execution` 已进一步形成 `QueryProcessor + QueryCompiler + BuiltInCommandHandler + StatementTableNameResolver + ExecutionEngine + QueryExecutorBuilder` 的执行协作结构
- 当前 `core.sql` 已形成 `Planner + Select/Insert/Delete/UpdatePlanBuilder` 与 `SemanticAnalyzer + Select/Insert/Delete/UpdateSemanticValidator` 的初步协作结构
- 当前 `core.sql` 已进一步形成 `CreateTable/CreateIndex/AlterTable/DropTable/CreateUser/Grant` 级别的 builder / validator 协作者，`Planner` 与 `SemanticAnalyzer` 已接近纯语句分发层
- 当前测试主结构已按 `core.sql / core.execution / core.storage / core.transaction / access.protocol` 收口，目录语义开始与主代码一致
- 终端和 Maven 的 JDK 版本曾存在不一致，需要坚持 Java 21
- 当前 `web/` 已具备可构建的前端工程，并已优先调用 Spring Boot `query / trace / history` 接口，接口不可用时才回退到 mock 数据
- 当前前后端已按分离部署思路收口：前端通过 `VITE_API_BASE_URL` 访问后端，后端通过 `app.web.cors.allowed-origins` 控制跨域
- 当前 `app` 返回给前端的表格数据已来自 `core.execution.QueryResult`，不再依赖 `rawResult` 文本解析
- 当前 Web UI 的 trace 已开始使用运行时真实命中组件，而不是只靠阶段级推断
- 当前系统层已明确收口为 `storage / transaction / catalog` 共同构成的基础设施层，运行时默认组装已不再直接散落在 `QueryRuntime`
- 当前日志与锁管理已开始从“具体实现类直连”转向“接口 + 默认实现”结构，单机默认实现仍由 `LogManager`、`LockManager` 提供
- 当前运行时装配和数据库文件定位也已开始从“本地路径写死”转向“接口 + 本地默认实现”结构
- 当前 `Catalog` 的默认权限重载路径也已纳入同一套路径解析策略，不再单独硬编码 `default` 库文件位置
- 当前 `Catalog` 不再自己承担临时磁盘访问与权限重载细节，目录层开始形成更明确的协作者结构
- 当前 `Catalog` 也不再直接绑定唯一的元数据存取实现，目录元数据边界开始明确
- 当前 `Catalog` 的用户目录和索引目录也已具备接口边界，目录层协作者结构进一步稳定
- 当前 `Catalog` 的默认协作者装配也已从构造器内部散落 `new` 收口到显式装配入口
- 当前目录层已经开始从直接依赖 `BufferPoolManager` 转向依赖 `PageAccess`
- 当前恢复链路也已开始复用 `PageAccess`，系统层上半部分的页访问边界更一致
- 当前 `TableHeap` 也已开始复用 `PageAccess`，目录/恢复/表堆三条链路的页访问边界已统一
- 当前 Maven Wrapper 与跨平台脚本已恢复可用，并能在显式提供 `JAVA21_HOME` 时稳定切到 Java 21
- 当前编译器已开始按 PostgreSQL 方言组织关键字和类型解析，新增类型不再必须继续修改 `DataType.valueOf(...)` 链路
- 当前 `SMALLINT / BIGINT / TIMESTAMP` 已不只是语法别名，而是具备独立 `DataType` 与 `Value` 序列化支持
- 当前 `BOOLEAN / DATE / VARCHAR(n) / CHAR(n)` 已有 parser / planner / semantic 回归覆盖，`VARCHAR(0) / CHAR(0) / NUMERIC(4,8)` 会在语义阶段直接拒绝
- 当前 `REAL / FLOAT8 / NUMERIC / TEXT` 也已有 parser / planner / semantic / integration 回归覆盖，`TEXT(10)` 会被直接拒绝
- 当前 `VARCHAR(n) / CHAR(n)` 的长度参数已经不会在建表后丢失，数据库重启后仍能继续拦截超长 `INSERT / UPDATE`
- 当前 MySQL 协议兼容层返回的列类型展示也已开始保留真实长度信息，`SHOW FULL COLUMNS` 与 `SHOW CREATE TABLE` 不再丢失 `VARCHAR(n) / CHAR(n)`
- 当前 `NUMERIC(p,s)` 也已不只是编译期参数，数据库重启后仍能继续拦截精度超限和小数位超限的 `INSERT`
- 当前 `NUMERIC(p,s)` 的精度/scale 约束也已覆盖 `UPDATE`，协议层 `SHOW CREATE TABLE` 已能输出 `PRIMARY KEY (...)`，`SHOW FULL COLUMNS` 也会为主键列返回 `PRI`
- 当前 `NOT NULL / DEFAULT / PRIMARY KEY` 已不再只存在于建表语句文本里，列约束元数据会随 catalog 一起持久化并在数据库重启后恢复
- 当前 MySQL 协议兼容层的 `SHOW CREATE TABLE / SHOW FULL COLUMNS / information_schema.columns` 已开始复用列约束和索引元数据，而不是只展示类型名

## 用户已确定的总体规划

0. 修复启动入口，要跨平台，易部署
1. 重构整个项目底层代码，改用更合理的设计模式
2. 将 client 启动入口明确分为 CLI、GUI、Web UI、Navicat 兼容四个入口
3. 加入 Web 网页设计，并与 Spring Boot 结合
4. 引入网络编程、Redis 等，使用 submodule 组织更复杂能力
5. 自动化测试、管理与 CI/CD 工作流逐步完善，部署放在最后完成

## 当前主要待办

1. 继续扩展 PostgreSQL 方言支持，优先补齐剩余数据类型、关键字和 DDL/查询语法兼容
2. 继续把编译器改造成注册式、可扩展结构，降低新增语句和类型的改动面
3. 补齐编译链路回归测试，锁住 PostgreSQL 方言收口结果
4. 将现有入口进一步收口到统一 launcher
5. 开始数据库内核与 Spring Boot 适配层的边界强化
6. 再评估是否拆成 `rill-core` / `rill-app` 两个 Maven 模块

## 编译器后续完整清单

### 方言目标

- 以 PostgreSQL 方言为主线
- 不再继续混入 MySQL 风格语义

### 关键字与词法

- 扩展 PostgreSQL 关键字与保留字
- 继续下沉词法关键字注册表
- 统一大小写和标识符处理规则

### 语法结构

- 继续把 `Parser` 拆成注册式语句解析结构
- 为语句级解析下沉独立 parser / handler
- 收口 DDL 语法，重点是类型、列定义和 `ALTER TABLE`

### 类型系统

- 优先补 `INTEGER / REAL / DOUBLE PRECISION / NUMERIC / TEXT / BOOLEAN / DATE / VARCHAR(n) / CHAR(n)` 的边界行为，并继续评估更完整的 PostgreSQL 类型能力
- 区分语法别名与内部物理类型
- 逐步摆脱 `DataType.valueOf(...)` 式耦合

### 语义与计划

- 继续拆分类型校验、表达式校验、权限校验
- 收紧类型兼容规则
- 让计划层只依赖归一化后的类型结果

### 测试

- 为关键字、类型、多单词类型、DDL 和错误分支补回归测试
- 形成“新增语句/类型必须补哪些测试”的固定模板

## 最近一次变更

- 完成了 DDL 列约束与协议展示的一轮补齐：`NOT NULL / DEFAULT / PRIMARY KEY` 已打通 parser、planner、`Column`、catalog 持久化与重启恢复，`SHOW CREATE TABLE` 也开始输出普通索引 `KEY`
- 影响范围：`core.sql.lexer`、`core.sql.parser`、`core.sql.semantic`、`core.sql.planner`、`core.model`、`core.catalog`、`access.protocol`、`core.sql`/`access.protocol` 测试
- 当前结果：`ParserTest / PlannerTest / SemanticAnalyzerTest / DataTypeTest / MysqlProtocolHandlerTest` 已通过，协议层 `SHOW CREATE TABLE / SHOW FULL COLUMNS / information_schema.columns` 已能展示 `NOT NULL / DEFAULT / PRIMARY KEY / KEY`
- 下一步建议：继续补更完整的列约束与 DDL 方言能力，例如多列索引、唯一约束、`TIMESTAMP WITH TIME ZONE`，以及 `ALTER TABLE` 的更完整列约束变体

## 当前建议顺序

1. 先修入口和启动模型
2. 再拆 demo 逻辑
3. 再把旧入口改成兼容层或移除
4. 再做底层架构重构
5. 再做 Web UI 和网络化扩展
6. 最后补齐测试、管理和 CI/CD

## 风险提示

- 如果在启动结构还没稳定前就大量加功能，项目会更难维护
- 如果继续保留重复入口，后续文档和运行方式会持续分裂
- 如果过早引入复杂外部组件，底层重构会被打断
- 如果不持续更新文档，agent 协作质量会快速下降

## 文档更新规则

每次代码变更完成后，至少更新本文件，说明：

- 改了什么
- 影响了哪些模块
- 当前结果
- 下一步建议
