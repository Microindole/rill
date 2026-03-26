# 当前状态

## 当前阶段

阶段名称：内核第一轮重构已基本完成，当前重心切回系统层边界抽象与工程收口

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

## 用户已确定的总体规划

0. 修复启动入口，要跨平台，易部署
1. 重构整个项目底层代码，改用更合理的设计模式
2. 将 client 启动入口明确分为 CLI、GUI、Web UI、Navicat 兼容四个入口
3. 加入 Web 网页设计，并与 Spring Boot 结合
4. 引入网络编程、Redis 等，使用 submodule 组织更复杂能力
5. 自动化测试、管理与 CI/CD 工作流逐步完善，部署放在最后完成

## 当前主要待办

1. 继续重构数据库内核，优先收紧系统层边界，逐步抽出存储/目录等最小接口，并继续补目录/恢复/存储回归测试
2. 将现有入口进一步收口到统一 launcher
3. 开始数据库内核与 Spring Boot 适配层的边界强化
4. 继续完善 Web UI，接入 `/api/query/history` 历史面板，并优化 trace 展示
5. 继续完善 `app` 层的 controller / service / dto 结构
6. 再评估是否拆成 `rill-core` / `rill-app` 两个 Maven 模块

## 最近一次变更

- 完成了工程收尾：`mvnw.cmd / mvnw` 已恢复，`scripts/build.* / scripts/rill.*` 已统一优先使用 `JAVA21_HOME`
- 影响范围：构建入口、启动脚本、运行模块文档
- 当前结果：`mvnw.cmd -v` 可在 `JAVA21_HOME` 下显示 Java 21，`scripts/build.cmd` 与 `scripts/rill.cmd help` 已验证通过
- 下一步建议：继续评估哪些执行器仍必须直接依赖 `BufferPoolManager`，或者转回入口统一与 `app/core` 边界强化

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
