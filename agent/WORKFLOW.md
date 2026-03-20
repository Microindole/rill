# Agent 工作流

## 基本原则

本项目的 agent 工作方式不是“只改代码”，而是“代码与文档同步演进”。

## 开始任务前

每次进入任务时，至少执行以下动作：

1. 读取 `agent/ENTRY.md`
2. 读取 `agent/STATUS.md`
3. 根据任务读取 `agent/ARCHITECTURE.md`
4. 根据任务读取相关模块文档和 skills 文档

## 执行任务时

- 先确认当前任务属于哪个模块
- 先判断是否会影响运行方式、架构边界或维护规则
- 代码修改时，不只考虑“能跑”，还要考虑是否会破坏整体收敛方向

## 结束任务前必须检查

### 必更文档

- `agent/STATUS.md`

### 条件更新文档

满足以下条件时必须更新：

- 改了运行方式或入口：更新 `agent/ARCHITECTURE.md` 和 `agent/modules/runtime.md`
- 改了编译链路：更新 `agent/modules/compiler.md`
- 改了执行链路：更新 `agent/modules/engine.md`
- 改了存储或索引：更新 `agent/modules/storage.md`
- 改了事务或恢复：更新 `agent/modules/transaction.md`
- 改了客户端、协议或工具：更新 `agent/modules/clients_and_tools.md`
- 形成新的固定工作套路：更新 `agent/skills/*.md`

## 文档更新最小模板

更新状态文件时，至少写清：

- 本次变更
- 影响范围
- 当前结果
- 下一步建议

## 禁止事项

- 改代码但不改文档
- 新增入口但不更新运行文档
- 修改架构边界但不更新架构文档
- 长期依赖口头上下文而不沉淀到 `agent` 目录
