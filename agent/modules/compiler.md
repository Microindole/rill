# 编译模块

## 关注范围

本模块负责 SQL 的编译前半段处理，主要包括：

- `core.compiler.lexer`
- `core.compiler.parser`
- `core.compiler.semantic`
- `core.compiler.planner`
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
- 当前新增语句类型时，优先新增规则注册和专属处理器，而不是继续扩写单一方法
