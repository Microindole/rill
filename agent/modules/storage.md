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
- 数据库路径与目录重载此前偏写死，当前正在逐步抽成可替换策略

## 后续重点

- 明确存储层职责边界
- 补强索引与目录元数据说明
- 补充更多工程化约束和验证策略
- 继续减少目录层对本地文件布局的隐式依赖

## 当前测试结构

- `DatabaseManagerTest` 已迁入 `src/test/java/com/indolyn/rill/core/storage/database`
- B+Tree 与索引相关测试已迁入 `src/test/java/com/indolyn/rill/core/storage/index`
- 页面替换策略测试已迁入 `src/test/java/com/indolyn/rill/core/storage/buffer/replacement`
- `CatalogTest` 已迁入 `src/test/java/com/indolyn/rill/core/catalog`，开始直接覆盖目录元数据、用户与权限持久化重载行为

## 当前重构进展

- 已新增 `DatabasePathResolver` 与 `LocalDatabasePathResolver`
- `DatabaseManager` 已可接收路径解析器
- `Catalog` 的默认权限重载已开始复用同一套路径解析策略
- `Catalog` 的权限重载逻辑已下沉到 `PermissionReloadAccess`
- 已新增 `CatalogMetadataAccess`，`CatalogMetadataStore` 作为当前默认页式元数据访问实现保留
- 已新增 `UserDirectoryAccess` 与 `IndexCatalogAccess`，目录层的用户目录与索引目录也开始显式分边界
- 已新增 `CatalogCollaborators`，目录层默认协作者装配已开始集中化
- 已新增 `PageAccess`，`BufferPoolManager` 作为当前默认页访问实现保留
- 目录层与恢复链路已经开始复用 `PageAccess`
- `TableHeap` 与执行支撑层也已开始复用 `PageAccess`
