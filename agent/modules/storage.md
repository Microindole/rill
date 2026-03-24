# 存储模块

## 关注范围

本模块记录页式存储、缓冲池、索引与目录元数据相关能力。

主要组件：

- `core.storage.database.DatabaseManager`
- `core.catalog.*`
- `core.storage.disk.DiskManager`
- `core.storage.buffer.BufferPoolManager`
- `core.storage.page.*`
- `core.storage.index.*`

## 当前价值

这是项目最能体现系统实现深度的部分之一。

## 当前问题

- 仍偏实验实现，后续需要增强边界清晰度和可维护性
- 文档和约束尚未完全跟上代码复杂度

## 后续重点

- 明确存储层职责边界
- 补强索引与目录元数据说明
- 补充更多工程化约束和验证策略
