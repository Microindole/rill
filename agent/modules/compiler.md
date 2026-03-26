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

## 当前价值

这是项目区别于普通 CRUD 项目的关键部分之一，是面试中非常重要的技术亮点。

## 后续重点

- 提升语法与语义支持完整度
- 规范错误信息
- 继续拆解 `SemanticAnalyzer` 与 `Planner` 内部职责
- 将语义规则、计划构建规则进一步下沉为独立处理器/工厂
- 为优化器改造预留空间

## 当前重构进展

- `Planner` 已从单一超长 `instanceof` 分发改为注册式语句计划映射
- `SemanticAnalyzer` 已从单一超长条件分支改为注册式语义规则分发
- `SELECT` 语句的计划构建已下沉到 `SelectPlanBuilder`
- `SELECT` 语句的语义检查已下沉到 `SelectSemanticValidator`
- `INSERT / UPDATE / DELETE` 的计划构建已分别下沉到独立的 DML builder
- `INSERT / UPDATE / DELETE` 的语义检查已分别下沉到独立的 DML validator
- 单表 DML 的权限、列校验、字面量类型兼容规则已收口到 `SemanticValidationSupport`
- `CREATE / ALTER / DROP / CREATE INDEX / CREATE USER / GRANT` 的计划构建已继续下沉到独立 builder
- `CREATE / ALTER / DROP / CREATE USER / GRANT` 的语义校验已继续下沉到独立 validator
- root 权限要求与列定义类型校验已收口到 `DefinitionValidationSupport`
- `ShowTables` 已并入统一 validator 注册体系，`Planner` 与 `SemanticAnalyzer` 中大部分桥接方法已被移除
- 对应测试目录已收口到 `src/test/java/com/indolyn/rill/core/sql`，原先按 `ddl / dcl` 打散的 SQL 集成测试已迁入 `core.sql.integration`
- `PlannerTest`、`SemanticAnalyzerTest` 已改为 JUnit 5，并开始直接约束当前 builder / validator 结构下的关键行为
- 当前新增语句类型时，优先新增规则注册和专属处理器，而不是继续扩写单一方法
- 当前 `SemanticAnalyzer` 与 `Planner` 已接入运行时 trace 埋点，会在实际分发时记录命中的 validator / builder 组件
