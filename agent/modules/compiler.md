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
- 梳理 Planner 的职责边界
- 为优化器改造预留空间
