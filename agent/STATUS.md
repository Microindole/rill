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
- 明确过渡策略为“先拆目录，再收口到多模块构建”
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
- 完成 GitHub 自动化基础设施第一轮落地：新增后端 Maven + 前端 Vite 的 CI workflow，并补齐 Dependabot 对 GitHub Actions / Maven / npm 的更新策略
- 完成 GitHub 自动化基础设施第二轮收口：CI 已加入路径变更检测、最小只读权限和前端构建产物上传，README 也已补齐 CI / Dependabot 说明
- 完成 GitHub 自动化基础设施第三轮补强：PR 现在会额外执行 dependency review，依赖更新不再只靠构建是否通过来兜底
- 完成 GitHub 自动化基础设施第四轮补强：CI 已扩为 Win/Linux/macOS 跨平台矩阵，并拆成跨平台构建层 + Linux 核心回归层
- 完成 GitHub 自动化基础设施第五轮收口：CI 已按主编排 / 后端矩阵 / 后端回归 / 前端矩阵拆分成多个 workflow 文件，避免单文件继续膨胀
- 完成 GitHub 自动化基础设施第六轮补强：跨平台矩阵已加入 GitHub-hosted ARM64 runner，覆盖 Linux ARM64 / Windows ARM64 / macOS ARM64
- 完成编译器第六轮收口：`Parser` 的语句注册与 DDL/SHOW 解析已下沉到独立协作者，新增语句入口不再继续堆回单个解析类
- 完成编译器第七轮收口：`SELECT / INSERT / UPDATE / DELETE` 与表达式、列定义/类型引用解析也已继续拆出协作者，`Parser` 已基本退化为 token 驱动外壳
- 完成 GitHub 发布流水线第一轮落地：新增 tag 驱动的 release workflow，可分别产出 Windows 安装包、纯 `rill-app-web` jar，以及带前端静态资源的 `rill-app-web` jar
- 完成 Windows 安装包第一轮分层：安装器内固定包含数据库内核服务端与 CLI，可选包含 GUI 和 MySQL/Navicat 兼容服务端，并随包携带精简 Java runtime
- 完成 Web 发布物第一轮分层：`rill-app-web` 新增 `with-ui` profile，能够在保留纯后端 jar 的同时，单独生成内嵌 `web/dist` 的一体化部署 jar
- 完成平台安装脚本目录第一轮补齐：`packaging/windows|linux|macos/bin` 现在都具备独立 launcher 脚本，平台专用发布资产边界已明确
- 完成 GitHub 发布流水线第二轮补齐：tag 发布已新增 Linux / macOS 平台归档包，不再只有 Windows 安装包和 Web jar
- 完成 agent 文档体系第一轮同步修正：`ENTRY / ARCHITECTURE / ARCHITECTURE_TARGET / WORKFLOW / runtime / app / clients_and_tools / ROADMAP` 已与当前多模块、发布边界和 Web 打包形态对齐

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
- 当前仓库已经有可执行的 GitHub CI，会在 push / pull request 上校验后端编译、当前维护中的核心回归套件，以及前端 `npm run build`
- 当前 Dependabot 已不再是空配置，已覆盖 GitHub Actions、根目录 Maven 依赖和 `web/` 下的 npm 依赖
- 当前 CI 已能按变更路径跳过不相关 job，减少仅改文档或单侧代码时的无效构建
- 当前 PR 也已开始执行依赖变更审查，Dependabot 和人工依赖升级都能得到一层额外风险检查
- 当前 GitHub CI 已覆盖 `ubuntu-latest / windows-latest / macos-latest / ubuntu-24.04-arm / windows-11-arm`，能够更早发现脚本路径、wrapper、换行和平台兼容问题
- 当前 GitHub CI 结构也已从“单个大 workflow”收口为“主入口 + 可复用子 workflow”，后续继续扩 job 时不会继续挤在一个文件里
- 当前 ARM64 覆盖使用的是 GitHub public 仓库可直接使用的标准 GitHub-hosted runner，不依赖 self-hosted 机器
- 当前跨平台后端 job 已不再只做 `compile`，而是会对多模块执行 `package` 并上传模块化工件，后续接 `exe/jpackage` 构建时边界更清楚
- 当前 GitHub Release 已开始按“数据库主体安装包”和“Web 控制台 jar”分开产物，而不是重新退回单体全家桶发布
- 当前 Windows 安装包已经具备组件化边界：`core + cli` 固定安装，`gui + mysqlcompat` 可选安装
- 当前 `rill-app-web` 已能同时产出纯后端 jar 和带前端静态资源的单文件 jar
- 当前 `packaging/windows|linux|macos/bin` 已作为发布资产纳入版本控制，而不是构建临时文件
- 当前 tag 发布已覆盖 Windows 安装包、Linux 归档包、macOS 归档包，以及纯 Web jar / 带 UI Web jar
- 当前 `agent/` 主文档已不再停留在“暂不拆多模块”阶段，文档与仓库当前结构基本一致
- 当前 `Parser` 已从“单类集中负责语句注册 + DDL 解析 + DML 解析 + 表达式解析”进一步收口到“核心 token 游标 + DML/表达式解析 + 独立语句协作者”
- 当前 `Parser` 已进一步收口为“token 游标 + 通用 match/consume + 协作者入口”，语句解析、表达式解析、DDL 类型/列定义解析都已不再集中堆在一个类里

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
6. 继续收口新的多模块边界，并为后续分布式层预留扩展位置

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

- 完成了 GitHub 自动化基础设施第二轮补齐：CI 新增路径过滤、最小权限和前端产物上传，README 也已补上 CI / Dependabot 使用说明
- 影响范围：`.github/workflows`、`.github/dependabot.yml`、`README.md`、`agent/STATUS.md`
- 当前结果：仓库的 GitHub 自动化已经从“可运行”提升到“可协作使用”，可以按改动范围更精准地执行前后端校验
- 下一步建议：继续评估测试矩阵、分支保护配套和更细粒度的 PR 失败提示
- 完成了编译器第六轮收口：新增 `ParserStatementRegistry`、`DefinitionStatementParsers` 和独立 `StatementParser` 接口，把语句入口注册和 DDL/SHOW 解析从 `Parser` 主类中拆出
- 影响范围：`core.sql.parser`、`agent/STATUS.md`、`agent/modules/compiler.md`
- 当前结果：`./mvnw.cmd -q -DskipTests compile` 与 `ParserTest / PlannerTest / SemanticAnalyzerTest` 已通过，后续新增 `CREATE / SHOW / DROP / USE / GRANT` 相关入口时改动面继续缩小
- 下一步建议：继续把 `SELECT` 的子句解析和 DML 解析按同样思路拆出，最终让 `Parser` 只保留 token 驱动和通用表达式能力
- 完成了编译器第七轮收口：新增 `QueryStatementParsers`、`ExpressionParsers`、`TypeDefinitionParsers`，把 `SELECT / INSERT / UPDATE / DELETE`、表达式与 `ColumnDefinition / TypeReference` 解析都从 `Parser` 主类继续拆出
- 影响范围：`core.sql.parser`、`core.sql` 测试、`agent/STATUS.md`、`agent/modules/compiler.md`
- 当前结果：`./mvnw.cmd -q -DskipTests compile` 与 `ParserTest / PlannerTest / SemanticAnalyzerTest / DataTypeTest / MysqlProtocolHandlerTest` 已通过，编译器“新增关键字/语句时集中改一个巨类”的问题已继续明显缓解
- 下一步建议：编译器入口层这条线已经接近可接受形态，后续更值得继续做的是历史失败测试收口、更多 PostgreSQL 语法支持，以及更细粒度的 parser 子句单测
- 完成了 GitHub 自动化基础设施第四轮补齐：CI 新增 Win/Linux/macOS 矩阵后端构建、Win/Linux/macOS 矩阵前端构建，并保留 Linux 单平台核心回归套件
- 影响范围：`.github/workflows/ci.yml`、`README.md`、`agent/STATUS.md`
- 当前结果：仓库已经具备真正的多平台 CI，而不是只在 Linux 上做单平台验证
- 下一步建议：继续评估是否增加 ARM64 / self-hosted runner、发布产物打包、测试结果汇总和分支保护规则
- 完成了 GitHub 自动化基础设施第五轮补齐：新增 `backend-cross-platform.yml`、`backend-regression.yml`、`frontend-cross-platform.yml`，主 `ci.yml` 只保留触发、变更检测、依赖审查和 workflow 编排
- 影响范围：`.github/workflows/*.yml`、`README.md`、`agent/STATUS.md`
- 当前结果：CI 文件结构已经更易维护，后续扩矩阵、加发布、加安全检查时不会继续把一个文件撑大
- 下一步建议：继续评估发布 workflow、nightly workflow 和更细粒度的测试报告汇总
- 完成了 GitHub 自动化基础设施第六轮补齐：`backend-cross-platform.yml` 与 `frontend-cross-platform.yml` 已补入 `ubuntu-24.04-arm / windows-11-arm`，形成 x64 + ARM64 的 GitHub-hosted 公共仓库矩阵
- 影响范围：`.github/workflows/*.yml`、`README.md`、`agent/STATUS.md`
- 当前结果：CI 现在不只覆盖三大桌面平台，也开始显式覆盖 ARM64 兼容性；`macos-latest` 继续承担 macOS ARM64 线
- 下一步建议：继续评估是否需要增加 Intel macOS 对照、nightly 全量测试和发布产物打包
- 完成了 GitHub 自动化基础设施第七轮补齐：修正多模块后的路径过滤，并把跨平台后端 job 从 `compile` 提升到 `package + artifact upload`
- 影响范围：`.github/workflows/ci.yml`、`.github/workflows/backend-cross-platform.yml`、`README.md`、`agent/STATUS.md`
- 当前结果：多模块改动现在能稳定触发 CI；`rill-server / rill-client / rill-app-web / rill-launcher` 工件会随矩阵构建上传，后续接 `jpackage` / `exe` 打包更自然
- 下一步建议：继续把 Windows GUI 的 `jpackage` 产物单独做成 desktop packaging workflow，并补 release/nighly 产物归档
- 完成了 GitHub 自动化基础设施第八轮补齐：新增 `release.yml`、`release-desktop-windows.yml`、`release-web-jars.yml`，tag 发布时会自动构建 Windows 安装包、纯 `rill-app-web` jar 和带 UI 的 `rill-app-web` jar
- 影响范围：`.github/workflows/*.yml`、`packaging/windows/**`、`rill-app-web/pom.xml`、`README.md`、`agent/STATUS.md`
- 当前结果：发布流已经按产品边界拆开；Windows 安装包固定交付数据库主体与 CLI，可选交付 GUI / MySQL 协议兼容服务，Web 控制台继续保持独立 jar 形态
- 下一步建议：继续补 GitHub Release 产物校验、Linux/macOS 安装包和版本号注入策略，避免长期只停在 Windows 安装器一条线
- 完成了 GitHub 自动化基础设施第九轮补齐：新增 `release-desktop-linux.yml`、`release-desktop-macos.yml`，tag 发布时会同时构建 Linux x64 与 macOS ARM64 的桌面/本地归档包
- 影响范围：`.github/workflows/*.yml`、`README.md`、`agent/STATUS.md`
- 当前结果：tag 发布产物已经具备平台对应版本；Windows 走安装器，Linux/macOS 走内置 runtime 的归档包，Web 继续保持 jar 分发
- 下一步建议：继续评估是否把 Linux 归档进一步升级为 `deb/rpm`，以及把 macOS 归档升级为 `.app/.dmg`
- 完成了 agent 文档体系第一轮补齐：统一修正 `agent/` 下关于多模块、Web UI、发布流水线和安装边界的滞后表述
- 影响范围：`agent/ENTRY.md`、`agent/ARCHITECTURE*.md`、`agent/WORKFLOW.md`、`agent/modules/*.md`、`agent/foundation/ROADMAP.md`、`agent/STATUS.md`
- 当前结果：`agent` 文档已与当前仓库事实基本对齐，不再残留“尚未拆多模块”或“Web UI 尚未正式建立”这类过期结论
- 下一步建议：后续凡是改动发布结构、安装模型或模块边界，必须同步扫描 `agent` 主文档和相关模块文档，避免再次出现系统性滞后

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

- 完成运行与构建结构第六轮收口：仓库已切到父 `pom` 聚合的多模块结构，形成 `rill-core / rill-server / rill-client / rill-app-web / rill-launcher`
- 影响范围：根 `pom.xml`、各子模块 `pom.xml`、源码与测试目录、`scripts/rill.*`、README 与运行/架构文档
- 当前结果：`./mvnw.cmd -q -DskipTests compile`、`./mvnw.cmd -q -pl rill-core,rill-server -am -Dsurefire.failIfNoSpecifiedTests=false "-Dtest=ParserTest,PlannerTest,SemanticAnalyzerTest,DataTypeTest,MysqlProtocolHandlerTest" test`、`./mvnw.cmd -q -DskipTests package` 已通过；服务端、客户端、Spring Boot Web 壳已开始分别产出模块化工件
- 下一步建议：继续把 CI、发布流程和后续分布式预留层也显式按模块组织，避免重新退回单体打包思路
