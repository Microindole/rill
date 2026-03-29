# 当前状态

## 当前快照（2026-03-29）

- 多模块结构、统一启动入口、edition 化发布和 `rill-app-web` 双形态 jar 已基本稳定
- `rill-core` 旧测试已不再维护，但新的测试体系已经重新覆盖基础设施、存储、编译器、执行层和一批真实集成场景
- 当前 `mvnw.cmd -q -pl rill-core -am verify` 已通过，`rill-core` 已重新成为可持续扩展的测试基线
- Codecov 已接通，当前只展示 `rill-core` 覆盖率，不设置失败门槛
- `ALTER TABLE` parser 主链已经接通，并补上了 parser / planner / integration 回归
- `rill-core` 测试已进一步扩到 `BPlusTree` 深边界、未提交 `UPDATE/DELETE` 恢复回滚、跨页恢复、聚合函数、表生命周期和类型约束失败路径
- `rill-core` 测试已继续补到主键冲突、索引列更新生命周期、已提交 `UPDATE/DELETE` 恢复保持，以及 `ALTER TABLE` 后列元数据/默认值的重启持久化
- `rill-app-web` 已开始从“查询壳”升级为“工作台后端”，新增 workspace/session 应用层和统一 JSON 错误处理
- 当前最高优先级已切换为：把 `rill-app-web` 做成面试主叙事中的正式后端产品层
- 当前已经明确：Spring Boot 后端后续将采用“业务库 PostgreSQL + MyBatis-Plus”与“自研 `rill-core` 内核能力”双链路并存的结构
- `rill-app-web` 已完成第一批 PostgreSQL + MyBatis-Plus 接入，工作台 session 不再只是内存态
- `rill-app-web` 已补出第二批业务对象 `demo_scenario`，开始把“业务 CRUD”与“调用自研内核执行场景脚本”真正串起来
- `rill-app-web` 已补出 `workspace dashboard` 和默认种子数据，后台开始具备“总览 + 默认演示素材”能力
- `rill-app-web` 已补出 `export_task`，开始把“执行 SQL”与“交付 csv/json 导出结果”连接起来

## 当前阶段

阶段名称：内核第一轮重构已基本完成，新 core 测试体系已建立第一轮主骨架，当前重心正式转向 Spring Boot 工作台后台、业务 CRUD 与双后端编排

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
- 完成 Spring Boot 后端调用边界第一轮收口：`rill-app-web` 已新增 `DatabaseService / EmbeddedDatabaseService / DatabaseExecution`，Web 层开始通过正式应用层边界调用 `rill-core`
- 完成 Spring Boot 模块测试清理第一轮收口：新增应用层 service 单测，并把模块内测试生成的 `data/` 目录纳入忽略，避免局部测试污染工作区
- 完成 Web UI 演示台第一轮升级：前端已从单页 SQL 控制台扩展为 SQL、模块结构、能力摘要、扩展路线并列展示，后端新增 `overview` 接口提供摘要数据
- 完成数据库内核系统语句测试补齐第一轮：`Parser / Planner / Semantic / QueryProcessor` 已补上 `CREATE / SHOW / USE / DROP DATABASE` 回归，开始收口重构后最容易漏测的系统语句路径
- 完成客户端工具入口第一轮收口：`DataReader / LogReader / ShellRunner` 已去掉独立 `main`，工具能力开始转为可复用组件，由 `rill-launcher` 或 GUI 调用
- 完成统一命令入口第一轮收口：scripts/rill.* 与 RillLauncher 已开始使用 sql / log / data / web 作为正式子命令，client / spring / data-reader / log-reader 降级为兼容别名
- 完成发布与 CI 命令层补齐：release 包已新增统一 rill 包装命令，backend 跨平台 CI 也开始对 help / log / data / web 做 smoke test
- 完成发布包装脚本能力检测收口：desktop 发行版的 ill 命令不再假设所有组件都存在，缺失组件时会明确报错；ill-app-web 的独立发布定位也已明确为单独分发但内含 core 依赖

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
- 当前 `app` 已开始承载工作台 session、当前数据库和最近查询这类前端应用状态，而不只是薄 controller 转发
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
- 当前 `app` 已开始提供 `/api/workspace/sessions` 这一类正式工作台接口，为前端后续切换到“有状态工作台”结构做准备
- 当前 `app` 已开始提供 `sql snippet` 这类正式业务 CRUD 接口，Spring Boot 后端开始具备真正的后台数据模型
- 当前 `app` 已开始提供 `demo scenario` 的 CRUD 和运行接口，Spring Boot 后端开始具备“可回放演示脚本”的产品能力
- 当前 `app` 已开始提供 `workspace dashboard` 总览接口，能给前端展示 session、历史、snippet、scenario 的整体状态
- 当前 `app` 已开始提供 `export task` 的 CRUD 和运行接口，能从后台直接触发查询导出
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
- 当前仓库已经有可执行的 GitHub CI，会在 push / pull request 上校验后端编译、多平台打包、命令层 smoke test，以及前端 `npm run build`
- `rill-core` 的历史测试目录已整体下线，不再把明显过时、边界不清或耗时过长的旧 JUnit 继续挂在默认 CI 上；新的 core 测试将按当前模块边界重新逐步补回
- 新的 core 测试方向已明确为“双线收口”：一条从基础设施到集成逐层往外补，一条从真实 SQL / 真实请求往内压整条执行链路
- 新的 core 测试骨架已开始落地：第一批基线测试已覆盖基础设施模型、数据库目录、日志/锁/事务边界、编译器 smoke、`QueryCompiler` smoke，以及 `QueryProcessor` 的最小端到端链路
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
- 当前 `rill-app-web` 已不再需要让 Web 层直接贴着 `QueryProcessor` 组织调用，数据库访问边界已开始稳定为 `DatabaseService`
- 当前局部测试不再把 `rill-core/data/` 这类运行期副产物持续留在工作区
- 当前 Web UI 已不再只是“在浏览器里执行 SQL”，而是开始承担项目演示台职责
- 当前数据库内核对系统语句的覆盖已不再只停留在零散集成测试，核心单测链路也开始补齐
- 当前 `Parser` 已从“单类集中负责语句注册 + DDL 解析 + DML 解析 + 表达式解析”进一步收口到“核心 token 游标 + DML/表达式解析 + 独立语句协作者”
- 当前 `Parser` 已进一步收口为“token 游标 + 通用 match/consume + 协作者入口”，语句解析、表达式解析、DDL 类型/列定义解析都已不再集中堆在一个类里
- 当前客户端的数据导出、日志查看和存储演示工具已不再把独立 `main` 当成正式边界，`rill-client` 里保留的是可复用组件，启动壳已收口回 `rill-launcher`

## 用户已确定的总体规划

0. 修复启动入口，要跨平台，易部署
1. 重构整个项目底层代码，改用更合理的设计模式
2. 将 client 启动入口明确分为 CLI、GUI、Web UI、Navicat 兼容四个入口
3. 加入 Web 网页设计，并与 Spring Boot 结合
4. 引入网络编程、Redis 等，使用 submodule 组织更复杂能力
5. 自动化测试、管理与 CI/CD 工作流逐步完善，部署放在最后完成

## 当前主要待办

1. 按“基础设施 -> 存储/事务 -> 编译器 -> 执行 -> 通信 -> 集成”顺序重建 `rill-core` 测试体系
2. 同时补少量“由外到内”的真实 SQL / 请求场景回归，锁住主执行链路
3. 继续扩展 PostgreSQL 方言支持，优先补齐剩余数据类型、关键字和 DDL/查询语法兼容
4. 继续把编译器改造成注册式、可扩展结构，降低新增语句和类型的改动面
5. 将现有入口进一步收口到统一 launcher
6. 开始数据库内核与 Spring Boot 适配层的边界强化
7. 继续收口新的多模块边界，并为后续分布式层预留扩展位置

## 最新测试补强摘要

- 执行层已新增 `BuiltInCommandHandler / StatementTableNameResolver / QueryResultRenderer / ProjectionColumnResolver / PredicateFactory / ExpressionEvaluator / ExecutionSupport / command executors` 回归
- 编译器已新增 `PostgreSqlTypeResolver`、系统命令规划回归
- 集成链路已新增数据库命令、元数据命令、查询特性、权限命令、聚合函数、表生命周期、类型约束和 schema mutation 回归
- `SHOW CREATE TABLE` 已在 `rill-core` 真正打通，不再只是协议层单独展示
- `ShowTablesExecutor` 已修复对不可变列表做原地修改的问题
- `CommandPlanningTest` 已移到正确的 `execution` 包路径，`rill-core verify` 不再被测试类路径问题阻塞
- 恢复测试已覆盖未提交 `INSERT / UPDATE / DELETE` 和跨页宽 tuple 的 crash recovery 回滚
- 索引测试已覆盖叶子链顺序、根收缩、缺失键删除和收缩后再插入
- 主键约束测试已覆盖重复 `INSERT` 与主键更新冲突
- 索引生命周期测试已覆盖先建索引、再按索引列查询、再更新索引列后的再次查询
- `UpdateExecutor` 已修复按目标列类型解析整数字面量，`UPDATE ... SET id = 1` 不再落回 `SMALLINT/INT` 类型错配

## 测试重建大纲

新的测试体系按以下层次补：

1. 基础设施
2. 存储部分
3. 崩溃恢复引擎
4. B+Tree
5. 锁结构与事务行为
6. 编译器
7. SQL 执行
8. 通信层
9. 集成测试

并要求同时具备两种视角：

- 由内到外：先锁住内核不变量
- 由外到内：用真实 SQL / 真实请求验证整条链路

## 最近一次变更

- 新增 `codecov.yml`，把 `Codecov` 从默认展示切到显式配置：仅展示 `rill-core` 覆盖率、关闭 project/patch 门槛、忽略非核心模块与构建产物
- 影响范围：`codecov.yml`、`README.md`
- 当前结果：Codecov 页面会更明确地围绕 `rill-core` 展示，不再把前端、Spring Boot 壳、打包产物混进主覆盖率视图
- 下一步建议：继续补 `rill-core` 测试层次，让 `Flags / Files / Commits` 页面逐步具备更真实的参考意义

- 把新的 `rill-core` 测试体系继续推进到了存储层：新增 `Page / DiskManager / BufferPoolManager / BPlusTree / TableHeap` 基线测试
- 影响范围：`rill-core/src/test/java/com/indolyn/rill/core/storage/**`、`rill-core/src/test/java/com/indolyn/rill/core/execution/TableHeapBaselineTest.java`、`rill-core/src/test/TESTING.md`
- 当前结果：`./mvnw.cmd -q -pl rill-core -am verify` 已通过，存储层第一批不变量开始有正式回归保护
- 下一步建议：继续补“存储/事务”这一层的更细边界，优先是页替换策略、恢复明细场景，以及更接近真实索引分裂/合并的 B+Tree 回归
- 补了 Web API 的第一批 controller 测试：新增 `QueryControllerTest` 与 `OverviewControllerTest`，开始覆盖 HTTP 输入校验、history/trace 查询与 overview 返回
- 影响范围：`rill-app-web/src/test/java/com/indolyn/rill/app/web/**`
- 当前结果：`./mvnw.cmd -q -pl rill-app-web -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=EmbeddedDatabaseServiceTest,RillQueryServiceTest,OverviewControllerTest,QueryControllerTest" test` 已通过；本地全模块 `clean verify` 未通过的原因是 `rill-app-web/target/*.jar` 被外部进程占用，不是 controller 用例失败
- 下一步建议：继续补 `QueryTraceService` 和协议/网络层的测试，把“通信层”从 Web API 扩到真正的服务端访问边界
- 补了 `QueryTraceService` 的成功/失败行为测试，开始锁住 Web 演示台最核心的 lexer/parser/runtime trace 拼装逻辑
- 影响范围：`rill-app-web/src/test/java/com/indolyn/rill/app/service/QueryTraceServiceTest.java`
- 当前结果：`QueryTraceService` 现在对成功查询的 trace 组装、失败查询的 history/错误返回都有回归保护
- 下一步建议：继续把“通信层”扩到 `rill-server` 的更多协议细节和服务端访问边界，而不只是 Web 层 DTO 适配
- 扩了 `rill-server` 协议层回归：`MysqlProtocolHandlerTest` 继续覆盖元数据 helper，新增 `ServerEntryPointTest` 锁住 `server/mysql-server` 的端口解析规则
- 影响范围：`rill-server/src/test/java/com/indolyn/rill/access/protocol/**`
- 当前结果：服务端测试不再只覆盖一次握手和部分 DDL 展示，入口参数解析也开始受保护
- 下一步建议：继续补协议层 `SHOW/metadata` 的结果集行为，以及更接近真实连接命令流的 smoke
- 补了 `rill-launcher` 第一批测试：新增 `RillLauncherTest`，锁住统一入口的 `help` 和未知模式行为
- 影响范围：`rill-launcher/pom.xml`、`rill-launcher/src/test/java/com/indolyn/rill/app/boot/RillLauncherTest.java`
- 当前结果：launcher 不再完全裸奔，至少命令入口帮助和错误提示已有回归保护
- 下一步建议：后续如果要继续增强 launcher 测试，可以把 `sql/log/data` 的 `--help` 行为继续做成更明确的单元回归
- 补了系统命令 smoke：新增 `DatabaseCommandSmokeTest` 与 `MetadataCommandSmokeTest`，并修复 `SHOW CREATE TABLE` 在 `rill-core` 中返回空结果的问题
- 影响范围：`rill-core/src/test/java/com/indolyn/rill/core/integration/**`、`rill-core/src/main/java/com/indolyn/rill/core/execution/operator/command/ShowCreateTableExecutor.java`、`rill-core/src/main/java/com/indolyn/rill/core/execution/ExecutionEngine.java`
- 当前结果：`CREATE/SHOW/USE/DROP DATABASE` 与 `SHOW COLUMNS / SHOW CREATE TABLE` 现在都已有真实集成回归，核心执行链和协议层展示不再分裂
- 下一步建议：继续补协议层更接近真实命令流的 smoke，以及 `SHOW` 类系统语句在不同入口上的一致性验证

- 接入了 `rill-core` 覆盖率采集：新增 `JaCoCo` Maven 配置与 `core-coverage.yml`，CI 现在会在 Linux 上生成 `jacoco.xml` 并上传到 Codecov
- 影响范围：`pom.xml`、`rill-core/pom.xml`、`.github/workflows/ci.yml`、`.github/workflows/core-coverage.yml`、`README.md`、`rill-core/src/test/TESTING.md`
- 当前结果：覆盖率只用于展示 `rill-core` 重建进度和 PR 变化，不设置失败门槛，不会阻塞主 CI
- 下一步建议：继续按“基础设施 -> 存储/事务 -> 编译器 -> 执行 -> 通信 -> 集成”补测试，让 Codecov 曲线先具备真实参考价值

- 重建了 `rill-core` 第一批新测试骨架：新增 `src/test/TESTING.md`，并建立 `infrastructure / compiler / integration` 三层起始目录
- 新增第一批基线测试：`SchemaBaselineTest`、`DatabaseManagerBaselineTest`、`LogRecordBaselineTest`、`LogManagerBaselineTest`、`LockManagerBaselineTest`、`TransactionManagerBaselineTest`、`LexerParserBaselineTest`、`QueryCompilerBaselineTest`、`QueryProcessorSmokeTest`、`DmlMutationSmokeTest`、`RecoverySmokeTest`
- 当前结果：`./mvnw.cmd -q -pl rill-core -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=SchemaBaselineTest,DatabaseManagerBaselineTest,LogRecordBaselineTest,LogManagerBaselineTest,LockManagerBaselineTest,TransactionManagerBaselineTest,LexerParserBaselineTest,QueryCompilerBaselineTest,QueryProcessorSmokeTest,DmlMutationSmokeTest,RecoverySmokeTest" test` 已通过
- 下一步建议：继续补“由内到外”的下一层测试，优先是磁盘页/缓冲池、恢复明细场景，以及更明确的 SQL 执行行为测试

- 下线了 `rill-core` 历史测试目录，不再继续维护明显过时、边界不清和成本过高的旧 JUnit
- 主 CI 已同步切换为“构建 + 脚本 smoke test + 前端 build”，停止默认执行旧 core 回归测试
- 明确了新的测试重建方法：从基础设施到集成逐层补，同时补少量关键端到端场景

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
- 完成了 Spring Boot 后端边界第一轮补齐：新增 `DatabaseService / EmbeddedDatabaseService / DatabaseExecution`，`RillQueryService` 与 `QueryTraceService` 已改为依赖应用层数据库边界，而不是直接贴着内核执行器
- 影响范围：`rill-app-web/src/main/java/com/indolyn/rill/app/service/**`、`rill-app-web/src/test/java/**`、`agent/modules/app.md`、`agent/STATUS.md`
- 当前结果：Spring Boot 模块已经有更清晰的“应用层 -> 数据库内核”调用路径，后续替换为远程/分布式实现时不需要从 controller 开始返工
- 下一步建议：继续补 `rill-app-web` 的 service/controller 单测，并把历史、trace、错误模型也逐步按同一层次收口
- 完成了 Spring Boot 模块测试清理第一轮补齐：新增 `RillQueryServiceTest`，并把 `**/data/` 加入忽略，避免模块级测试把运行期数据库文件留在仓库工作区
- 影响范围：`.gitignore`、`rill-app-web/src/test/java/**`、`agent/modules/app.md`、`agent/STATUS.md`
- 当前结果：`rill-app-web` 新增的应用层边界现在有最小单测保护，局部测试后的工作区也更干净
- 下一步建议：继续给 `QueryTraceService` 和 controller 层补单测，并逐步把 core 集成测试的数据目录改成更显式的临时路径
- 完成了 Web UI 演示台第一轮补齐：新增 `OverviewService / OverviewController` 和前端 overview 数据链路，把页面从“单纯执行 SQL”升级为“SQL + 架构 + 能力 + 扩展路线”的项目演示台
- 影响范围：`rill-app-web/src/main/java/com/indolyn/rill/app/{dto,service,web}/**`、`web/src/**`、`agent/modules/{app,web}.md`、`agent/ARCHITECTURE.md`、`agent/STATUS.md`
- 当前结果：浏览器端现在不只适合演示单次 SQL 执行，也能承载对模块结构、发布边界、网络编程与 Redis 扩展路线的讲解
- 下一步建议：继续把 trace 做成完全动态，并补 `overview` / `query` controller 的单测，随后再评估是否引入更正式的 architecture / plan / catalog 专页

- 完成了 Spring Boot 工作台后端第一轮补齐：新增 `WorkspaceService`、`WorkspaceController` 与 `RestExceptionHandler`，把 session、当前数据库、最近查询和统一错误模型从零散 controller 逻辑中抽出
- 影响范围：`rill-app-web/src/main/java/com/indolyn/rill/app/{dto,service,web}/**`、`rill-app-web/src/test/java/com/indolyn/rill/app/{service,web}/**`、`agent/modules/app.md`
- 当前结果：`rill-app-web` 现在不再只是单次 query/trace 壳，而开始具备“有状态工作台后端”的正式应用层结构；`WorkspaceServiceTest` 与 `WorkspaceControllerTest` 已通过
- 下一步建议：继续把前端逐步切到 workspace/session 接口，再补统一 API 响应模型、查询收藏/模板和可讲的鉴权边界

- 完成了 Spring Boot 业务层第一轮真正落地：`rill-app-web` 已接入 PostgreSQL + MyBatis-Plus，新增 `workspace_session / query_history / sql_snippet` 三张业务表，并补出 `SqlSnippetService / SqlSnippetController`
- 影响范围：`rill-app-web/pom.xml`、`rill-app-web/src/main/resources/application.properties`、`rill-app-web/src/main/resources/schema.sql`、`rill-app-web/src/main/java/com/indolyn/rill/app/{config,persistence,service,web,dto}/**`、`rill-app-web/src/test/java/com/indolyn/rill/app/{service,web}/**`
- 当前结果：工作台 session 已开始持久化，`query_history` 会随 session 执行落库，`SQL snippet` 已具备列表/详情/新建/更新/删除的第一批正式 CRUD，`rill-app-web` 相关 service/controller 测试已通过
- 下一步建议：继续补 `demo_scenario / export_task` 等业务表，并让前端工作台开始切到 `workspace/snippets` 和持久化 session 接口

- 完成了 Spring Boot 业务层第二轮落地：新增 `demo_scenario` 业务表、`DemoScenarioService` 与 `DemoScenarioController`，开始支持“配置演示场景 -> 在 workspace session 上执行脚本”这条产品链路
- 影响范围：`rill-app-web/src/main/resources/schema.sql`、`rill-app-web/src/main/java/com/indolyn/rill/app/{dto,persistence,service,web}/**`、`rill-app-web/src/test/java/com/indolyn/rill/app/{service,web}/**`
- 当前结果：`demo scenario` 已具备列表/详情/新建/更新/删除/运行接口，能把业务表 CRUD 和 `rill-core` SQL 执行链路接起来，相关 service/controller 测试已通过
- 下一步建议：继续补 `export_task`、用户/鉴权边界和前端工作台对 `snippets/scenarios` 的正式接入

- 完成了 Spring Boot 业务层第三轮落地：新增 `WorkspaceDashboardService / WorkspaceDashboardController`，补出工作台后台总览；同时通过 `data.sql` 注入默认 snippet 和 demo scenario
- 影响范围：`rill-app-web/src/main/resources/{schema.sql,data.sql}`、`rill-app-web/src/main/java/com/indolyn/rill/app/{dto,service,web}/**`、`rill-app-web/src/test/java/com/indolyn/rill/app/{service,web}/**`
- 当前结果：Spring Boot 后端现在不仅有 CRUD 和场景执行，还能返回 session / history / snippet / scenario 的聚合视图，适合前端做正式工作台首页
- 下一步建议：继续补 `export_task` 和用户/鉴权边界，再让前端切到 `workspace/dashboard`、`workspace/sessions`、`workspace/snippets`、`workspace/scenarios`

- 完成了 Spring Boot 业务层第四轮落地：新增 `export_task` 业务表、`ExportTaskService / ExportTaskController`，支持把 SQL 查询结果导出为 csv/json 文件
- 影响范围：`rill-app-web/src/main/resources/schema.sql`、`rill-app-web/src/main/java/com/indolyn/rill/app/{dto,persistence,service,web}/**`、`rill-app-web/src/test/java/com/indolyn/rill/app/{service,web}/**`
- 当前结果：工作台后台已经具备 session、history、snippet、scenario、dashboard、export task 这一整批正式业务对象，导出任务运行时会实际生成文件并更新任务状态
- 下一步建议：继续补用户/鉴权边界、基于 PostgreSQL 的用户态工作台数据隔离，以及前端对 dashboard/snippet/scenario/export task 的正式接入
- 完成了数据库内核系统语句测试第一轮补齐：`ParserTest`、`PlannerTest`、`SemanticAnalyzerTest`、`QueryProcessorTest` 新增 `CREATE DATABASE / SHOW DATABASES / USE / DROP DATABASE` 回归，补上系统语句在 parser、planner、执行层的空白覆盖
- 影响范围：`rill-core/src/test/java/com/indolyn/rill/core/{sql,execution}/**`、`agent/modules/compiler.md`、`agent/STATUS.md`
- 当前结果：重构后最容易漏掉的“能解析但没有计划 / 没有执行 / 没有行为断言”的系统语句路径已经开始被锁住
- 下一步建议：继续沿同一模板把 `SHOW COLUMNS / SHOW CREATE TABLE / ALTER TABLE` 等系统/DDL 语句也补成 parser、planner、执行的成套回归
- 完成了测试体系新一轮补齐：`ALTER TABLE` parser 主链已接通，新增真实通信 smoke（原生 server / MySQL 握手 / CLI 命令流）、`BPlusTree` 分裂/合并结构回归、未提交与多事务交错恢复回归，以及失败路径 SQL 回归
- 影响范围：`rill-core/src/main/java/com/indolyn/rill/core/sql/parser/**`、`rill-core/src/main/java/com/indolyn/rill/core/transaction/LockManager.java`、`rill-core/src/test/java/com/indolyn/rill/core/{compiler,execution,infrastructure,integration,storage}/**`、`rill-server/src/test/java/com/indolyn/rill/access/protocol/**`、`rill-client/src/test/java/com/indolyn/rill/access/cli/**`、`rill-core/src/test/TESTING.md`
- 当前结果：`./mvnw.cmd -q -pl rill-core -am verify`、`rill-server` 协议 smoke、`rill-client` CLI flow、`rill-app-web` service/controller 测试均已通过；通信、索引、恢复和 DDL 主链现在都有正式回归保护
- 下一步建议：继续补更深的 `BPlusTree` 极端边界、多页恢复和更完整的子查询/DDL 能力；当前“测试骨架已建立”已推进到“关键缺口已补齐一轮”

## 当前建议顺序

1. 先把 `rill-app-web` 做成正式工作台后台与面试主叙事入口
2. 在 Spring Boot 层建立真实业务逻辑与 CRUD，业务数据优先落 PostgreSQL，并通过 MyBatis-Plus 管理
3. 让 `rill-core` 继续作为数据库内核能力层，通过正式应用层边界被 Spring Boot 调用
4. 继续补 `rill-core` 测试、通信层和恢复边界，作为 Spring Boot 产品层的技术底座
5. 再推进网络编程、Redis、分布式预留与更完整 Web 产品能力
6. 最后继续收口部署、CI/CD 与更完整发布体验

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
