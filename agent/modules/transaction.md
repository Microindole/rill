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
