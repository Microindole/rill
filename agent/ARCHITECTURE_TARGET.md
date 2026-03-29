# 目标架构

## 当前决策

当前阶段采用以下策略：

- 单仓库保留
- 已完成代码目录和职责边界拆分
- 已切到父 `pom` 聚合的多模块结构
- 继续在同一仓库内收口模块职责与发布边界
- 为后续分布式层和可选安装组件预留位置

## 当前快照

截至 2026-03-29，目标架构已经从“是否拆多模块”进入“如何让模块、发布、测试和后续分布式边界长期稳定，并把 Spring Boot 做成正式产品层”的阶段。

这意味着当前重点已经从“是否拆模块”转向“如何让模块边界、发布边界和后续分布式边界稳定”。

## 当前多模块结构

当前模块为：

- `rill-core`
- `rill-server`
- `rill-client`
- `rill-app-web`
- `rill-launcher`

当前产品边界为：

- 数据库主体：`rill-core + rill-server`
- 本地客户端：`rill-client`
- Web 控制台：`rill-app-web`
- 开发统一入口：`rill-launcher`

当前产品重点边界为：

- `rill-core`：数据库内核能力
- `rill-app-web`：工作台后台、业务 CRUD、内核编排层
- 外部 PostgreSQL：Spring Boot 业务数据存储

当前发布边界为：

- Windows：edition 包 `core-cli / desktop / mysql-compat`，并保留可选组件式安装器
- Linux/macOS：edition 化归档包 `core-cli / desktop / mysql-compat`，均带内置 runtime
- Web：`rill-web-api` 与 `rill-web-ui` 两种独立 jar

## 第一阶段目标目录

当前代码包结构仍然保留以下职责分层：

```text
src/main/java/com/indolyn/rill/
  app/
    boot/

  access/
    cli/
    gui/
    protocol/

  tools/

  core/
    catalog/
    model/
    exception/
    sql/
    execution/
    session/
    storage/
    transaction/
```

## 各层职责

### rill-app-web / app

Spring Boot 适配层与 Web 控制台服务壳。

职责：

- 应用启动
- Web UI 承载
- 后端服务层
- 工作台业务层
- 对 `rill-core` 的正式编排层
- 对外部 PostgreSQL 业务库的正式访问层
- 后续 controller / service / config 等应用层能力
- 可单独形成纯后端 jar
- 也可通过 `with-ui` profile 形成内嵌 `web/dist` 的单文件部署 jar

当前建议的应用层主结构：

- `web`：controller / dto / advice
- `service`：workspace、query、trace、overview、business service
- `infra.rill`：通过 `DatabaseService` 调用 `rill-core`
- `infra.rdbms`：通过 MyBatis-Plus 访问 PostgreSQL

### rill-client / access

外部访问层。

职责：

- CLI 终端入口
- GUI 桌面入口
- CLI 与 GUI 本地入口
- 本地工具入口

### rill-server / access.protocol

服务端与协议兼容入口。

职责：

- 原生 rill 服务端
- MySQL/Navicat 兼容服务端
- 后续服务端部署与协议扩展

### tools

辅助工具能力，当前主要并入 `rill-client` 范围内理解，不再作为独立产品发布线。

### rill-core / core

数据库内核主包。

- `core.catalog`
- `core.model`
- `core.exception`
- `core.sql`
- `core.execution`
- `core.session`
- `core.storage`
- `core.transaction`
- `core.storage.database`

这些是数据库本体。当前这一步已经完成第一阶段迁移，后续重点不再是“是否迁入 core”，而是“如何保证 core 不被外层反向污染”。

当前已完成的关键净化动作：

- `Session` 已下沉到 `core.session`
- `core.common` 已拆分为 `core.model` 与 `core.exception`
- `core.compiler` 已重命名为 `core.sql`
- `core.engine` 与 `core.executor` 已合并为 `core.execution`
- `core.sql.ast` 已从 `core.sql.parser.ast` 提升为独立中间层
- `core` 不再直接依赖 `access` / `tools` / `app`
- `rill-app-web` 已成为正式 Web 壳模块，而不是未来前身
- 系统层语义已开始明确为 `storage + transaction + catalog`
- `QueryRuntime` 的默认基础设施初始化已开始收口到专门组装器，而不是继续散落在运行时类中
- 运行时基础设施已开始抽成 `RuntimeInfrastructureFactory`
- 数据库文件定位已开始抽成 `DatabasePathResolver`
- 日志与锁管理已开始抽成 `LogService`、`LockService`

这几项不是为了现在就做分布式，而是为了避免把“本地磁盘 + 本地锁 + 本地日志”写死成唯一运行形态。

## 第二阶段可能演进

当前多模块已经完成，后续演进重点改为：

- 先把 `rill-app-web` 做成正式的工作台和业务后台
- 继续压实 `rill-core / rill-server / rill-client / rill-app-web` 的边界
- 为数据库主体安装包和可选组件形成更稳定的产品结构
- 为未来 `cluster/gateway/coordinator` 级别模块预留空间
- 继续避免让 Spring Boot 或客户端入口反向污染内核

当前业务层推荐路线已经明确：

- 业务 CRUD 优先落到 PostgreSQL
- ORM/DAO 组织优先采用 MyBatis-Plus
- `rill-core` 不直接承担这些业务表和后台 CRUD
- Spring Boot 负责把“业务后台”和“数据库内核演示/调用”两条链路编排起来

后续可能新增的模块方向包括：

- `rill-cluster-api`
- `rill-cluster-node`
- `rill-cluster-coordinator`
- `rill-sql-gateway`

## 测试目标结构

`rill-core` 的新测试体系也将按当前模块边界和系统层次重建，而不是恢复旧的历史目录。

目标分层为：

- 基础设施测试
- 存储与事务测试
- 编译器测试
- SQL 执行测试
- 通信层测试
- 集成测试

目标方法为“双线收口”：

- 一条线从系统内部往外测，先锁住基础设施和内核不变量
- 一条线从真实 SQL / 真实请求往内压，锁住整条执行链路

默认 CI 将只接入其中稳定、快速、边界清晰的子集；慢测试、性能测试、探索性测试不再直接混入默认后端 CI。

当前进度不是规划状态，而是已落地一部分：

- `rill-core` 新测试目录已经按 `infrastructure / storage / compiler / execution / integration` 主线重建
- `mvn -pl rill-core -am verify` 已经可以作为新的核心验证基线
- 通信层与更深的协议/端到端测试仍需继续补

## 当前结论

当前结论非常明确：

- 多模块已经拆完
- 当前重点不再是“要不要拆模块”，而是“如何让模块与产品边界长期稳定”
- 顶层目录和发布产物都应优先表达稳定职责，而不是回退到单体全家桶
- `rill-app-web` 保持独立发布，不进入数据库主体安装包
- `rill-app-web` 当前应被视为最高优先级模块
- Spring Boot 后端后续需要同时承载真实业务 CRUD 与自研数据库内核能力编排

当前已经进入“模块结构 + 发布结构 + 后续分布式预留”并行收口阶段。

## 系统层演进约束

后续系统层重构要遵守两个约束：

- 先抽最小替换边界，不提前实现分布式版本
- 默认单机实现必须始终可运行，不能为了抽象把项目做空

当前优先保留替换点的能力包括：

- 运行时基础设施组装
- 数据库路径/文件定位
- 日志服务
- 锁服务
- 后续的目录存储访问与页访问能力
