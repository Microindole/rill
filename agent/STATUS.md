# 当前状态

## 当前快照

- 仓库已稳定为 Maven 多模块：`rill-core`、`rill-server`、`rill-client`、`rill-app-web`、`rill-launcher`
- 当前主线已从数据库内核重构转向 `rill-app-web` 和 `web/` 的产品化，目标是作为 Java 面试项目主叙事
- `rill-core` 保留为技术底座和亮点，不再继续大规模重构

## 现在可讲的项目主线

- Spring Boot 工作台后端：用户、会话、历史、SQL 片段、场景脚本、导出任务
- PostgreSQL + MyBatis-Plus：业务数据持久化
- 自研数据库内核：SQL 编译、执行、恢复、协议兼容能力
- 前后端分离：Vue 3 工作台
- 中间件：Redis、RocketMQ、GitHub OAuth2

## 近期关键结果

- 认证链路已成型：账号密码、JWT、邮件验证、Turnstile、GitHub OAuth2 共存
- GitHub OAuth2 已支持“创建新账号 / 绑定已有账号”
- Redis 已接入短期认证状态存储
- RocketMQ 已接入导出任务异步链路，默认仍可退回 `local`
- Docker 部署目录和 Win11 / Debian 中间件启动文档已补齐
- 内置管理员已调整为 `root`，`demo` 回归普通用户
- 普通用户现在可访问 `default` 和自己的数据库；`DROP DATABASE` 仍仅 `root`
- 协议层已同步 `Session.currentDatabase`，避免切库后 core 权限误判
- `JwtAuthenticationFilter` 已修正，不再把真实异常伪装成 `Login required`
- parser 已放开 `use default;` 这类语句，`default` 可作为合法数据库名

## 当前已知注意点

- 老的 `data/<db>/rill.data.log` 可能与当前日志格式不兼容；若出现 `LogRecord deserialization failed`，优先备份后移走对应 `.log`
- RocketMQ 和 Redis 都是独立服务；生产部署时应视为常驻基础设施，不要绑进每次应用发版脚本
- `config/rill-app-secrets.properties` 是唯一真实敏感配置入口；不要把密码写回示例文件或 `docker-compose.yml`

## 建议的当前优先级

1. 继续优化 `rill-app-web` 与 `web/` 的工作台体验
2. 把 OAuth2、异步导出、管理员能力讲顺
3. 收口部署和演示路径，保证本地与 Debian 线上一致
4. 仅对 `rill-core` 做必要 bugfix，不再扩大战线

## 常用验证命令

- 后端打包：`./mvnw.cmd -pl rill-app-web -am package`
- 前端构建：`npm run build` in `web/`
- 中间件启动：`docker-compose up -d` in `deploy/services`

## 最近一次关键修正

- 修复 `use default;` 被错误解析为关键字导致的 500
- 修复认证过滤器吞异常导致的假 `Login required`
