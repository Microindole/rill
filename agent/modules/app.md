# 应用层模块

## 关注范围

本模块记录 Spring Boot 适配层，也就是 `rill-app-web` 模块承载的后端服务壳、Web UI 服务端与管理接口层。

## 当前组件

- `app.service.DatabaseService`
- `app.service.EmbeddedDatabaseService`
- `app.service.DatabaseExecution`
- `app.boot.RillApplication`
- `app.service.QueryProcessorRegistry`
- `app.service.RillQueryService`
- `app.service.QueryTraceService`
- `app.service.OverviewService`
- `app.web.HealthController`
- `app.web.OverviewController`
- `app.web.QueryController`
- `app.dto.*`
- `app.config.WebCorsConfig`

## 当前职责

当前 `app` 层的职责已经开始明确为：

- 承载 Spring Boot 启动
- 作为外层服务调用 `core`
- 为后续 Web UI / HTTP API 提供后端壳

## 当前状态

目前 `app` 层还只是最小骨架，但方向已经成立：

- `app` 不再等于数据库本体
- `app` 已开始通过 `DatabaseService -> EmbeddedDatabaseService -> QueryProcessorRegistry -> QueryProcessor` 这条链调用 `core`
- 已经有最小的 Web 控制器与服务层结构
- 已经开始提供 `overview` 接口，向前端输出模块结构、能力摘要和扩展路线
- `web/` 已起出第一版 Vue 前端骨架，当前已由 `app` 提供真实查询、trace、历史记录接口
- 当前后端跨域来源已收口为配置项 `app.web.cors.allowed-origins`
- 当前 `app` 已通过应用层数据库边界消费内核结构化结果，不再让 Web 层直接持有 `QueryProcessor`
- 当前 `rill-app-web` 已支持两种发布形态：
  - 纯 Spring Boot jar
  - 通过 `with-ui` profile 内嵌 `web/dist` 的单文件 jar
- 当前 `rill-app-web` 已开始补应用层单测，先锁住 `DatabaseService` 边界与 `RillQueryService` 的委托关系

## 后续重点

- 增加更正式的 controller / service / dto 结构
- 避免 controller 直接操作 `core`
- 建立统一的应用层错误处理与响应模型
- 为 Web UI 提供 `query / trace / history / health` 等正式接口
- 为 Web UI 提供 `overview` 这类非 SQL 摘要接口，让演示台不只依赖单次查询结果
- 后续逐步把当前“阶段级 trace 推断”升级为执行链路真实埋点
- 保持前后端分离部署能力，不写死前端地址
- 同时允许以可选 profile 的方式把前端静态资源打入 jar，作为单文件部署形态
- 后续继续把 `DatabaseService` 稳定成 Spring Boot 调用内核的唯一正式边界，为未来远程/分布式实现预留替换点
- 后续网络编程和 Redis 也应优先通过 app/service 层或新的 gateway/coordinator 模块接入，而不是反向污染 `rill-core`


