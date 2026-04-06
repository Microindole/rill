# 应用层模块

## 关注范围

本模块记录 Spring Boot 适配层，也就是 `rill-app-web` 模块承载的后端服务壳、Web UI 服务端与管理接口层。

## 当前组件

- `app.service.DatabaseService`
- `app.service.QueryProcessorRegistry`
- `app.service.RillQueryService`
- `app.service.QueryTraceService`
- `app.service.OverviewService`
- `app.service.WorkspaceService`
- `app.service.WorkspaceDashboardService`
- `app.service.SqlSnippetService`
- `app.service.DemoScenarioService`
- `app.service.ExportTaskService`
- `app.service.CurrentUserProvider`
- `app.service.JwtService`
- `app.service.DatabaseAccessPolicyService`
- `app.service.AuthService`
- `app.service.AuthenticatedUser`
- `app.service.JwtPrincipal`
- `app.service.DatabaseExecution`
- `app.boot.RillApplication`
- `app.service.impl.*`
- `app.security.*`
- `app.controller.HealthController`
- `app.controller.OverviewController`
- `app.controller.QueryController`
- `app.controller.AuthController`
- `app.controller.WorkspaceController`
- `app.controller.WorkspaceDashboardController`
- `app.controller.SqlSnippetController`
- `app.controller.DemoScenarioController`
- `app.controller.ExportTaskController`
- `app.controller.RestExceptionHandler`
- `app.config.MybatisPlusConfig`
- `app.persistence.entity.*`
- `app.persistence.mapper.*`
- `app.dto.*`
- `app.config.WebCorsConfig`

## 当前职责

当前 `app` 层的职责已经开始明确为：

- 承载 Spring Boot 启动
- 作为外层服务调用 `core`
- 承载工作台业务与业务 CRUD
- 编排业务数据库与 `rill-core` 两条后端链路
- 为后续 Web UI / HTTP API 提供后端壳
- 承载工作台会话、当前数据库和最近查询这类应用层状态

## 当前状态

目前 `app` 层还只是最小骨架，但方向已经成立：

- `app` 不再等于数据库本体
- `app` 已开始通过 `DatabaseService -> EmbeddedDatabaseService -> QueryProcessorRegistry -> QueryProcessor` 这条链调用 `core`
- 已经有最小的 Web 控制器与服务层结构
- 已经开始提供 `overview` 接口，向前端输出模块结构、能力摘要和扩展路线
- `web/` 已起出第一版 Vue 前端骨架，当前已由 `app` 提供真实查询、trace、历史记录接口
- 当前后端跨域来源已收口为配置项 `app.web.cors.allowed-origins`
- 当前 `app` 已通过应用层数据库边界消费内核结构化结果，不再让 Web 层直接持有 `QueryProcessor`
- 当前 `app` 已补出 `WorkspaceService`，开始把“前端工作台会话状态”从 controller 里抽到应用层
- 当前 `app` 的目标模型已经明确成双后端：
  - 业务链路：`Controller -> Service -> MyBatis-Plus -> PostgreSQL`
  - 内核链路：`Controller -> Service -> DatabaseService -> rill-core`
- 当前 `app` 已开始真正接入 PostgreSQL + MyBatis-Plus，而不再只是停留在架构说明层
- 当前 `WorkspaceController` 已支持：
  - 会话列表
  - 创建工作台 session
  - 查询 session 当前数据库与最近查询
  - 在 session 上执行 SQL
  - 查询指定 session 的历史记录
  - 删除 session
- 当前 `WorkspaceDashboardController` 已开始提供后台总览：
  - 总 session 数
  - 总 query history 数
  - snippet / scenario 数量
  - 最近查询
  - 当前 session 摘要
- 当前 `WorkspaceService` 已从纯内存实现切到持久化实现：
  - `workspace_session`
  - `query_history`
- 当前 `SqlSnippetController` 已补出第一批业务 CRUD：
  - 列表
  - 详情
  - 新建
  - 更新
  - 删除
- 当前 `DemoScenarioController` 已补出第二批业务 CRUD 与执行能力：
  - 列表
  - 详情
  - 新建
  - 更新
  - 删除
  - 在指定 workspace session 上执行场景 SQL 脚本
- 当前 `ExportTaskController` 已补出第三批业务 CRUD 与运行能力：
  - 列表
  - 详情
  - 新建
  - 更新
  - 删除
  - 运行导出任务并生成 csv/json 文件
  - 导出任务当前已拆成“业务 service 入队 + 独立处理器执行”结构，可通过 `local / rocketmq` 两种传输模式驱动
- 当前 `app` 已开始补出正式前后端分离认证边界：
  - `app_user`
  - `app_jwt_session`
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/auth/me`
  - `DELETE /api/auth/logout`
  - `Authorization: Bearer <jwt>` 维持登录态
- 当前用户模型已经开始和自研数据库内核的数据库资源绑定：
  - 游客只允许访问共享 `default`
  - 注册用户会自动分配一个同名内核数据库
  - 管理员是唯一允许创建/删除内核数据库的角色
- 当前 `app` 已开始承担“PostgreSQL 平台业务数据 + 自研数据库内核数据面”的双后端编排职责
- 当前 `workspace_session / query_history / sql_snippet / demo_scenario / export_task` 已开始按 `owner_id` 做用户隔离，不再默认把所有工作台资产混在一个全局空间里
- 当前包结构已开始按职责收口：
  - `app.controller.*` 负责 HTTP 接口
  - `app.service.*` 负责接口定义
  - `app.service.impl.*` 负责实现
  - `app.security.*` 负责 JWT 过滤和请求级用户上下文
  - `app.persistence.*` 负责 PostgreSQL 持久化
- 当前认证职责已继续收口：
  - `AuthService` 负责登录 / 注册 / 注销
  - `JwtAuthenticationFilter` 负责解析 `Authorization: Bearer <jwt>`
  - `CurrentUserProvider` 只负责读取当前用户，不再自己从 request 抠 token
- 当前应用层写路径已开始补事务边界：
  - `AuthService`
  - `WorkspaceService`
  - `SqlSnippetService`
  - `DemoScenarioService`
  - `ExportTaskService`
- 当前密码存储已切到 `BCryptPasswordEncoder`：
  - 注册时写入 bcrypt 哈希
  - 登录时用 `PasswordEncoder.matches(...)` 校验
  - 默认 `demo / guest` 用户改成启动期自动补种和自动迁移旧明文
- 当前认证链又往前推进了一轮：
  - 登录已支持“普通验证”模式的验证码接入点，默认推荐 Cloudflare Turnstile
  - 注册不再直接发登录态，而是发送带时效性的邮箱验证链接
  - 修改密码需要先验证当前密码，再发送邮箱确认链接
  - 忘记密码通过注册邮箱发送带时效性的重置链接
  - Cloudflare Turnstile 当前仍作为登录人机校验保留，不替代系统内部短期验证 token
  - 邮箱验证 / 改密确认 / 找回密码 token 已开始具备 `database / redis` 可切换存储能力，后续可继续把高频认证短状态迁到 Redis
  - GitHub OAuth2 登录已开始接入，当前采用“GitHub 完成认证 -> 本地 `app_oauth_account` 绑定/建号 -> 签发现有 JWT”模式，而不是用 Spring Session 直接替代业务登录态
  - 当 GitHub 身份尚未绑定时，后端会生成短期 OAuth2 pending state，由前端登录页引导用户选择“创建新账号”或“绑定已有账号”
  - GitHub `client-id / client-secret` 延续现有敏感配置约定，通过 `config/rill-app-secrets.properties` 注入
  - 管理员接口已开始独立到 `app.controller.AdminUserController`
  - Spring Boot 已开始通过 `config/rill-app-secrets.properties` 外部化敏感配置
- 当前异步任务链路也已开始起步：
  - `export_task` 已新增消息发布边界与独立执行处理器
  - 默认 `local` 传输仍可在单机开发环境直接运行
  - 配置切到 `rocketmq` 后可通过 RocketMQ producer / consumer 驱动异步导出
- 当前 `data.sql` 已开始提供默认 snippet 和默认 demo scenario，方便本地和演示环境开箱即用
- 当前 `RestExceptionHandler` 已开始把 `ResponseStatusException` 收口成统一 JSON 错误模型
- 当前 `rill-app-web` 已支持两种发布形态，且两者都会通过 Maven 依赖携带 `rill-core`：
  - 纯 Spring Boot jar
  - 通过 `with-ui` profile 内嵌 `web/dist` 的单文件 jar
- 当前 `rill-app-web` 已开始补应用层单测，先锁住 `DatabaseService` 边界与 `RillQueryService` 的委托关系
- 当前 `QueryTraceService`、`QueryController`、`OverviewController`、`WorkspaceService`、`WorkspaceDashboardService`、`WorkspaceController`、`WorkspaceDashboardController`、`SqlSnippetService`、`SqlSnippetController`、`DemoScenarioService`、`DemoScenarioController`、`ExportTaskService`、`ExportTaskController` 也已经有测试，Web 后端不再只靠手工联调

## 后续重点

- 增加更正式的 controller / service / dto / repository 结构
- 保持 `service` 作为接口层，`service.impl` 作为实现层，避免接口和实现继续堆在同一目录
- 避免 controller 直接操作 `core`
- 避免主代码直接依赖 `*Impl`，统一通过接口注入
- 保持认证过滤、JWT 解析和业务 service 解耦，避免业务层继续自己解析请求头
- 建立统一的应用层错误处理与响应模型
- 为 Web UI 提供 `query / trace / history / health` 等正式接口
- 为 Web UI 提供 `overview` 这类非 SQL 摘要接口，让演示台不只依赖单次查询结果
- 为 Web UI 提供“工作台会话 + 当前数据库 + 最近查询”的正式后端模型，而不是只暴露一次性 query 接口
- 以 PostgreSQL + MyBatis-Plus 建立真实业务数据模型，优先包括：
  - 工作台 session
  - SQL 收藏 / 模板 / 历史
  - 演示场景
  - 导出任务 / 查询记录
- 继续把当前“JWT + owner_id 隔离”升级成更正式的用户态后台：
  - 用户 CRUD
  - 更安全的密码存储
  - token 生命周期、刷新与撤销策略
  - 基于用户隔离工作台资产
- 继续把管理员后台和用户自助认证流程补完整：
  - 用户列表 / 用户详情
  - 用户数据库分配与删除
  - 验证码联调
  - 邮件模板和真实 SMTP 环境联调
  - 前端登录/注册/改密/忘记密码完整页面
- 继续把“游客 / 用户 / 管理员”的数据库访问边界落实到前端控制台和后台管理界面
- 继续把首页 / 登录页 / 控制台 / 项目介绍页收口成完整站点，而不是单页 SQL 控制台
- 把当前已落地的 `workspace_session / query_history / sql_snippet / demo_scenario` 继续向前端工作台接通
- 让 `export_task` 和 `workspace/dashboard` 也进入前端首页与资产管理视图
- 把 Spring Boot 做成面试时可讲的“正式后台”，而不是只转发 SQL 的壳
- 后续逐步把当前“阶段级 trace 推断”升级为执行链路真实埋点
- 保持前后端分离部署能力，不写死前端地址
- 同时允许以可选 profile 的方式把前端静态资源打入 jar，作为单文件部署形态
- 后续继续把 `DatabaseService` 稳定成 Spring Boot 调用内核的唯一正式边界，为未来远程/分布式实现预留替换点
- 后续网络编程和 Redis 也应优先通过 app/service 层或新的 gateway/coordinator 模块接入，而不是反向污染 `rill-core`


