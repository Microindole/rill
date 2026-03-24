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
- `ExecutionEngine` 历史上存在超长计划分发方法，扩展成本高

## 后续重点

- 稳定执行链路边界
- 继续拆分执行器工厂、表达式求值与谓词构建职责
- 为更强的查询能力和调试能力打基础

## 当前重构进展

- `ExecutionEngine` 已从巨型 `instanceof` 分发改为注册式执行器工厂映射
- 当前新增 `PlanNode` 类型时，可以优先增加工厂注册与专属构建方法，避免继续膨胀单一方法
