# 系统层模块

## 目标定位

本模块代表数据库内核中的基础设施能力，而不是 SQL 编译或业务语义本身。

当前应纳入系统层理解的部分：

- `core.storage`
- `core.transaction`
- `core.catalog`

## 当前职责

- 页式存储与磁盘访问
- 缓冲池与页面替换
- 索引页与 B+Tree
- 锁管理
- 事务管理
- 日志与恢复
- 元数据持久化与目录基础设施

## 当前问题

- 虽然 `storage / transaction / catalog` 已经拆包，但“系统层”这一顶层语义还没有被明确命名
- 运行时初始化仍然偏写死，`QueryRuntime` 直接 new 具体实现
- 未来如果要为分布式、多节点、远程存储留余地，当前边界还不够稳定

## 当前重构方向

- 先把系统层从“散落的实现目录”收口为清晰的基础设施概念
- 先抽最小运行时依赖边界，而不是一开始就做复杂接口体系
- 先保证默认单机实现可用，再考虑未来替换点

## 第一批优先抽象点

- 数据库运行时基础设施组装
- 存储路径与数据库文件定位
- 锁管理入口
- 日志服务入口
- 目录初始化入口

## 当前实施策略

- 第一阶段：引入默认基础设施组装器，减少 `QueryRuntime` 对具体实现的硬编码
- 第二阶段：识别哪些能力需要抽接口，哪些继续保留为默认实现
- 第三阶段：再评估是否把系统层进一步收口为更明确的 `infrastructure` 语义

## 当前进展

- 已新增 `DefaultRuntimeInfrastructureFactory`
- 已新增 `RuntimeInfrastructure`
- `QueryRuntime` 已改为通过默认基础设施组装器拿到 `DatabaseManager / DiskManager / BufferPoolManager / Catalog / LogManager / LockManager / TransactionManager`
- 已新增 `LogService`，默认由 `LogManager` 实现
- 已新增 `LockService`，默认由 `LockManager` 实现
- 已新增 `RuntimeInfrastructureFactory`，默认由 `DefaultRuntimeInfrastructureFactory` 实现
- 已新增 `DatabasePathResolver`，默认由 `LocalDatabasePathResolver` 实现
- `TransactionManager / RecoveryManager / RecoveryApplier / QueryProcessor / ExecutionEngine / TableHeap` 已切到 `LogService` 与 `LockService`
- `QueryRuntime` 已可接收可替换的运行时基础设施工厂
- `DatabaseManager` 已可接收可替换的路径解析器
- `Catalog` 已可接收可替换的路径解析器，默认权限重载不再独立硬编码本地路径
- `Catalog` 的权限重载已下沉到 `PermissionReloadAccess`
- `Catalog` 的元数据读写已开始下沉到 `CatalogMetadataAccess`
- `Catalog` 的用户目录与索引目录已开始下沉到 `UserDirectoryAccess`、`IndexCatalogAccess`
- `Catalog` 的默认协作者装配已开始下沉到 `CatalogCollaborators`
- 目录层已开始依赖 `PageAccess`，不再要求协作者直接绑定 `BufferPoolManager`
- 恢复链路已开始依赖 `PageAccess`
- `TableHeap` 与执行支撑层已开始依赖 `PageAccess`

## 当前结论

- 运行时组装已经不再散落在 `QueryRuntime`
- 日志与锁管理已经不再要求上层硬依赖具体实现类
- 数据库文件定位已经不再只能依赖硬编码本地路径
- 目录模块的默认权限重载路径也已开始与系统层路径策略统一
- 目录模块已经开始从“直接开磁盘读默认库”转向“目录编排层 + 权限重载协作者”
- 目录模块已经开始从“直接绑定页式元数据存储实现”转向“目录编排层 + 元数据访问协作者”
- 目录模块已经开始从“单类统一持有所有目录细节”转向“编排层 + 多个目录协作者”
- 目录模块已经开始从“构造器内部散落默认实现”转向“显式默认装配入口”
- 页访问能力已经开始从“直接暴露缓冲池实现”转向“接口 + 默认实现”
- 当前 `PageAccess` 已覆盖目录层与恢复链路，但执行/表堆侧还存在桥接点
- 当前 `PageAccess` 已覆盖目录层、恢复链路和 `TableHeap`，页访问边界开始在系统层上半部分统一
- 系统层还没有完全模块化，但已经开始具备“接口 + 默认单机实现”的基本骨架

## 下一步建议

- 继续抽存储路径定位与数据库文件装配边界
- 评估是否为目录基础设施引入更明确的存储访问边界
- 在接口抽象继续推进前，补一轮事务与恢复相关测试，锁住当前行为
