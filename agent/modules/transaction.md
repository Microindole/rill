# 事务模块

## 关注范围

本模块负责事务管理、锁管理、日志与恢复。

主要组件：

- `core.transaction.TransactionManager`
- `core.transaction.Transaction`
- `core.transaction.LockManager`
- `core.transaction.log.*`
- `core.transaction.RecoveryManager`

## 当前价值

这是项目从“普通课程作业”走向“系统型项目”的关键能力之一。

## 后续重点

- 明确事务边界与状态流转
- 梳理日志与恢复链路
- 明确当前保证和未保证的语义

## 当前重构进展

- `RecoveryManager` 已进一步退化为恢复编排层，物理 redo / undo 操作已下沉到 `RecoveryApplier`
- 已新增 `RecoveryTest` 回归测试，覆盖“未提交事务刷盘后重启恢复”的主路径
- 已修复恢复阶段每条日志使用假事务加锁但未释放的问题，redo/undo 不再在同一次恢复中自阻塞
- 已修复 `UPDATE` 日志撤销不完整的问题，undo 现在会恢复旧槽位并删除更新阶段插入的新 tuple
- 已新增 `LogService`，事务管理与恢复链路已不再直接要求依赖 `LogManager`
- 已新增 `LockService`，事务管理与恢复链路已不再直接要求依赖 `LockManager`
- 已新增 `PageAccess`，恢复链路已开始不再直接要求依赖 `BufferPoolManager`
- 恢复链路中通过 `TableHeap` 回落到默认缓冲池实现的桥接已移除

## 当前测试状态

- 事务层新测试已重新覆盖 `LogRecord / LogManager / LockManager / TransactionManager`
- 恢复链路已有新的 `RecoverySmokeTest`
- 事务与恢复行为现在已经重新进入 `rill-core verify` 主基线
