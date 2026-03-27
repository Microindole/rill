# 应用层模块

## 关注范围

本模块记录 Spring Boot 适配层，也就是 `rill-app-web` 模块承载的后端服务壳、Web UI 服务端与管理接口层。

## 当前组件

- `app.boot.RillApplication`
- `app.service.QueryProcessorRegistry`
- `app.service.RillQueryService`
- `app.service.QueryTraceService`
- `app.web.HealthController`
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
- `app` 开始通过 service 层调用 `core`
- 已经有最小的 Web 控制器与服务层结构
- `web/` 已起出第一版 Vue 前端骨架，当前已由 `app` 提供真实查询、trace、历史记录接口
- 当前后端跨域来源已收口为配置项 `app.web.cors.allowed-origins`
- 当前 `app` 已通过 `QueryProcessor.executeStructured(...)` 消费内核结构化结果，不再解析文本表格生成前端返回数据

## 后续重点

- 增加更正式的 controller / service / dto 结构
- 避免 controller 直接操作 `core`
- 建立统一的应用层错误处理与响应模型
- 为 Web UI 提供 `query / trace / history / health` 等正式接口
- 后续逐步把当前“阶段级 trace 推断”升级为执行链路真实埋点
- 保持前后端分离部署，不在 `app` 中耦合前端静态资源或写死前端地址


