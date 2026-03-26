# 执行模块

## 关注范围

本模块负责从计划节点到执行器树，再到结果返回的完整执行路径。

主要组件：

- `core.execution.QueryProcessor`
- `core.execution.ExecutionEngine`
- `core.execution.operator.*`
- `core.execution.operator.TupleIterator`

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
- `WHERE`/过滤谓词的 AST 到执行谓词转换已下沉到 `PredicateFactory`
- `QueryProcessor` 已拆出 `QueryCompiler`、`BuiltInCommandHandler`、`StatementTableNameResolver`
- `ExecutionEngine` 已拆出 `QueryExecutorBuilder`，查询类执行器的递归装配不再堆在总控类中
- 当前 `ExecutionEngine` 已按 command/query 两段注册执行器工厂
- 对应测试目录已收口到 `src/test/java/com/indolyn/rill/core/execution`，谓词下推测试已并入 `core.execution.optimization`
- 已新增 `QueryCompilerTest`、`QueryProcessorTest`，开始直接约束新拆出的执行协作者
- 已新增 `ExecutionEngineTest`，开始直接约束执行器工厂注册与不支持节点报错行为
- 当前查询类执行器测试需要显式事务，这与锁管理器约束保持一致
- 当前新增 `PlanNode` 类型时，可以优先增加工厂注册与专属构建方法，避免继续膨胀单一方法
- 当前 `ExecutionEngine` 已接入运行时 trace 埋点，会在实际命中执行器工厂时记录对应 executor 组件
