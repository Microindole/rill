# 架构总览

## 分层

1. `rill-core`：数据库内核（编译、执行、存储、事务、恢复）
2. `rill-server`：协议入口（原生 TCP / MySQL 协议）
3. `rill-client`：本地 CLI / GUI 客户端
4. `rill-app-web`：Web 后端（认证、会话、工作台业务）
5. `web`：前端控制台

## 主链路

1. 前端输入 SQL
2. `rill-app-web` 进行会话与权限校验
3. 调用查询服务进入 `rill-core`
4. 返回结果、耗时、trace
5. 前端展示结果/历史/资产（片段、场景、导出）

## 数据与状态

- 会话状态：`WorkspaceSession`
- 历史记录：`QueryHistory`
- SQL 片段：`SqlSnippet`
- 场景脚本：`DemoScenario`
- 导出任务：`ExportTask`

