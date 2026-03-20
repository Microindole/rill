# 执行模块

## 关注范围

本模块负责从计划节点到执行器树，再到结果返回的完整执行路径。

主要组件：

- `core.engine.QueryProcessor`
- `core.engine.ExecutionEngine`
- `core.executor.*`
- `core.executor.TupleIterator`

## 当前链路

AST -> PlanNode -> Executor Tree -> Tuple/Result

## 当前问题

- 执行层和其他层仍有部分耦合
- 查询处理与运行入口之间边界还不够清晰

## 后续重点

- 稳定执行链路边界
- 梳理执行器职责
- 为更强的查询能力和调试能力打基础
