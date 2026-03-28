# 编译模块

## 关注范围

本模块负责 SQL 的编译前半段处理，主要包括：

- `core.sql.lexer`
- `core.sql.parser`
- `core.sql.semantic`
- `core.sql.planner`
- AST 与 PlanNode 体系

## 当前主链路

SQL -> Token -> AST -> 语义检查 -> 物理计划

## 当前快照

- 词法、语法、语义、计划已经形成注册式主干
- PostgreSQL 类型解析器已有独立测试，不再只靠集成测试间接兜底
- 系统命令、元数据命令、权限命令的 parse/plan 行为也开始有独立回归
- `ALTER TABLE` 当前仍未完全接入 parser 主链，该现状已经由测试显式锁住

## 当前价值

这是项目区别于普通 CRUD 项目的关键部分之一，是面试中非常重要的技术亮点。

## 后续重点

- 提升语法与语义支持完整度
- 规范错误信息
- 继续拆解 `SemanticAnalyzer` 与 `Planner` 内部职责
- 将语义规则、计划构建规则进一步下沉为独立处理器/工厂
- 为优化器改造预留空间

## 完整改造清单

### 一. 方言目标与规则收口

- 明确项目编译器以后以 PostgreSQL 方言为准，不再继续按 MySQL 兼容扩展
- 区分“词法关键字”“语法结构”“语义规则”“内部物理类型”，避免继续混写
- 逐步清理当前偏 MySQL 的行为和输出，避免语义继续漂移

### 二. 词法层

- 继续扩展 `KeywordRegistry`，将关键字和保留字从 `Lexer` 本体中完全剥离
- 引入更清晰的 PostgreSQL 保留字清单，至少覆盖当前计划支持的 DDL / DML / 查询子句
- 为多单词类型和后续方言差异保留词法层最小扩展点
- 统一字面量、标识符、关键字的大小写处理规则

### 三. 语法层

- 继续把 `Parser` 从大方法拆成注册式语句解析器
- 将每类语句的解析职责继续下沉到独立 parser / handler，而不是继续把细节塞回 `Parser`
- 将列类型解析稳定为“类型引用 AST”，不再把语法名直接绑定到内部枚举
- 补齐 PostgreSQL 风格 DDL 语法支持，例如更多类型名、参数类型和更规范的 `ALTER TABLE` 变体
- 逐步清理当前不够标准的解析行为和宽松分支

### 四. 类型系统

- 继续扩展 `PostgreSqlTypeResolver`
- 第一批优先补齐：
  - `INTEGER`
  - `REAL`
  - `DOUBLE PRECISION`
  - `NUMERIC`
  - `TEXT`
  - `BOOLEAN`
  - `DATE`
  - `VARCHAR(n)`
  - `CHAR(n)`
- 区分“语法别名”和“内部物理类型”，避免继续直接 `DataType.valueOf(...)`
- 为后续 `TIMESTAMP / BIGINT / SMALLINT` 等真实物理支持预留内部模型改造点

### 五. 语义层

- 继续把类型校验、列定义校验、权限校验、表达式校验分离
- 让语义校验基于 `SqlTypeResolver` 和统一类型兼容规则工作
- 收紧类型兼容矩阵，避免当前“能跑但不标准”的宽松行为继续扩散
- 逐步把 PostgreSQL 语义约束写成独立 validator / support 组件

### 六. 计划层

- 让 `Planner` 完全只做注册和分发
- 计划构建器不再直接假定 SQL 语法名等于内部物理类型
- 继续为不同语句下沉独立 builder，避免中心类重新膨胀
- 为后续优化器、代价估计和更复杂计划改造预留稳定输入结构

### 七. 测试与验收

- 为 PostgreSQL 类型别名补 parser / semantic / planner 回归测试
- 为关键字、保留字和多单词类型补词法与语法回归测试
- 为错误分支补断言，确保报错信息和失败位置可预期
- 逐步建立“新增一种类型/一种语句时最少需要补哪些测试”的固定模板

## 当前阶段建议顺序

1. 扩 PostgreSQL 类型系统
2. 扩关键字与保留字
3. 收口 DDL 语法
4. 收口查询语法与语义
5. 补完整回归测试

## 当前阶段验收标准

- 新增一种 PostgreSQL 类型别名时，不需要同时改动 `Lexer / Parser / Semantic / Planner` 的硬编码链路
- 新增一类语句时，不需要继续扩写单一中心大方法
- 编译器对 PostgreSQL 目标支持清单有明确边界，而不是边写边猜
- 关键字、类型、语义、计划至少都有对应回归测试

## 当前已知硬边界

- `TIMESTAMP / BIGINT / SMALLINT` 这类类型如果要“完整支持”，后续一定会牵涉 `core.model.DataType`、`Value`、序列化和存储模型
- 因此编译器可以先支持语法与归一化，但真正落地完整类型能力时必须和系统层/模型层协同推进

## 当前重构进展

- `Planner` 已从单一超长 `instanceof` 分发改为注册式语句计划映射
- `SemanticAnalyzer` 已从单一超长条件分支改为注册式语义规则分发
- `Parser` 已开始从大 `if/else` 分发转向注册式语句解析分发
- `Lexer` 已引入 `KeywordRegistry`，默认关键字集已下沉到 `PostgreSqlKeywordRegistry`
- `SELECT` 语句的计划构建已下沉到 `SelectPlanBuilder`
- `SELECT` 语句的语义检查已下沉到 `SelectSemanticValidator`
- `INSERT / UPDATE / DELETE` 的计划构建已分别下沉到独立的 DML builder
- `INSERT / UPDATE / DELETE` 的语义检查已分别下沉到独立的 DML validator
- 单表 DML 的权限、列校验、字面量类型兼容规则已收口到 `SemanticValidationSupport`
- `CREATE / ALTER / DROP / CREATE INDEX / CREATE USER / GRANT` 的计划构建已继续下沉到独立 builder
- `CREATE / ALTER / DROP / CREATE USER / GRANT` 的语义校验已继续下沉到独立 validator
- root 权限要求与列定义类型校验已收口到 `DefinitionValidationSupport`
- DDL 列类型已不再直接依赖 `DataType.valueOf(...)`，新增了 `TypeReferenceNode + SqlTypeResolver + PostgreSqlTypeResolver`
- 当前 PostgreSQL 风格类型别名已开始支持 `INTEGER / TEXT / NUMERIC / DOUBLE PRECISION / CHARACTER VARYING`
- `ShowTables` 已并入统一 validator 注册体系，`Planner` 与 `SemanticAnalyzer` 中大部分桥接方法已被移除
- 对应测试目录已随多模块收口到 `rill-core/src/test/java/com/indolyn/rill/core/sql`，原先按 `ddl / dcl` 打散的 SQL 集成测试已迁入 `core.sql.integration`
- `PlannerTest`、`SemanticAnalyzerTest` 已改为 JUnit 5，并开始直接约束当前 builder / validator 结构下的关键行为
- `ParserTest`、`PlannerTest`、`SemanticAnalyzerTest` 已补上 PostgreSQL 类型别名回归测试
- `ParserTest`、`PlannerTest`、`SemanticAnalyzerTest`、`QueryProcessorTest` 已开始补系统语句回归，覆盖 `CREATE / SHOW / USE / DROP DATABASE` 这类最容易在重构后出现“能解析、不能执行”空洞覆盖的路径
- 当前 `SMALLINT / BIGINT / TIMESTAMP` 已从语法名打通到独立物理类型，`TIMESTAMP WITHOUT TIME ZONE` 已归一化到同一内部类型
- `DataTypeTest` 已补上真实建表、插入、回读的集成回归，覆盖 `SMALLINT / BIGINT / TIMESTAMP`
- 当前 `BOOLEAN / DATE / VARCHAR(n) / CHAR(n)` 也已补上 parser / planner / semantic 回归，`VARCHAR(0) / CHAR(0) / NUMERIC(4,8)` 会在类型解析阶段被拒绝
- 当前 `REAL / FLOAT8 / NUMERIC / TEXT` 也已补上 parser / planner / semantic / integration 回归，`TEXT(10)` 不会再错误继承 `VARCHAR` 的参数规则
- 当前 `Column` 已开始持有类型声明名和参数，`VARCHAR(n) / CHAR(n)` 的长度约束已能穿过 planner、catalog 持久化、重启恢复和 `TableHeap` 写入路径
- 当前外层协议展示也已开始复用同一套列声明信息，说明类型参数不再只停留在编译链路内部
- 当前 `NUMERIC(p,s)` 的精度/小数位限制也已开始复用同一套列参数信息，编译期和写入期都会拦截越界值
- 当前 `NUMERIC(p,s)` 的重启后回归已经覆盖到 `UPDATE`，外层 `SHOW CREATE TABLE / SHOW FULL COLUMNS` 也开始消费同一份主键和类型元数据
- 当前 `ColumnDefinitionNode` 已开始承载 `NOT NULL / DEFAULT / PRIMARY KEY` 这类列级约束，DDL 语法不再只保留类型名
- 当前 `Column` 已开始持有空值性、默认值和主键标记，这些元数据会穿过 planner、catalog 持久化和重启恢复
- 当前协议层 `SHOW CREATE TABLE` 已能输出 `NOT NULL / DEFAULT / PRIMARY KEY / KEY`，`SHOW FULL COLUMNS` 与 `information_schema.columns` 也开始复用同一份列约束与索引元数据
- 当前 `Parser` 的语句入口注册已经下沉到 `ParserStatementRegistry`，`CREATE / SHOW / DROP / USE / GRANT` 解析也已下沉到 `DefinitionStatementParsers`
- 这意味着新增 DDL / SHOW 级别语句时，已经不需要继续把入口分支和具体解析细节都堆回 `Parser` 一个类里
- 当前 `SELECT / INSERT / UPDATE / DELETE` 解析也已下沉到 `QueryStatementParsers`
- 当前表达式、聚合函数、`ORDER BY / LIMIT` 与基础主表达式解析已下沉到 `ExpressionParsers`
- 当前列定义和类型引用解析已下沉到 `TypeDefinitionParsers`
- 现在 `Parser` 已经更接近一个薄的编排层，而不是继续承担所有 SQL 语法细节
- 当前新增语句类型时，优先新增规则注册和专属处理器，而不是继续扩写单一方法
- 当前 `SemanticAnalyzer` 与 `Planner` 已接入运行时 trace 埋点，会在实际分发时记录命中的 validator / builder 组件

## 相关工程约束

- 编译模块相关回归现在已纳入 GitHub CI 的后端 Maven 校验链路，当前先使用维护中的核心回归套件，避免 parser / semantic / planner 的改动只在本地通过
- 前端 `web/` 已有独立构建步骤，因此涉及编译链路可视化或 Web UI 适配的改动也会受到 CI 中 `npm run build` 的约束
- 与编译模块联动的依赖升级 PR 现在也会经过 dependency review，降低编译链路在依赖变更时无感退化的风险
- 与编译模块联动的脚本和构建链路现在也会经过 x64 + ARM64 的 GitHub-hosted CI 验证，能更早发现 parser/compiler 相关改动在不同平台和架构上的路径与构建问题
- CI workflow 也已拆为多个文件，后续如果需要给编译模块单独增加 nightly/parser-only 回归，不必继续修改一个超大 CI 文件

