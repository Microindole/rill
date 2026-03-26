# Web UI 模块

## 目标定位

Web UI 不是项目介绍页，而是数据库内核的可视化控制台。

第一阶段目标：

- 在浏览器中执行 SQL
- 展示查询结果与错误信息
- 以流程图方式展示查询经过的执行阶段
- 展示本次查询命中的核心源码组件

## 推荐技术栈

- `Vue 3`
- `TypeScript`
- `Vite`
- `Pinia`
- `Vue Router`
- `Element Plus`
- `Tailwind CSS`
- `Vue Flow`

## 页面骨架

- `Workbench`：SQL 编辑、执行按钮、结果表格
- `Execution Flow`：词法/语法/语义/规划/执行的流程图视图
- `Source Insight`：展示命中的类、文件、职责说明
- `History`：最近执行记录与耗时

## 后端接口方向

第一阶段建议由 Spring Boot 提供：

- `POST /api/query/execute`
- `GET /api/query/history`
- `GET /api/trace/:traceId`
- `GET /api/health`

## Trace 事件模型

前端流程图不应直接扫描源码，而应消费后端返回的结构化 trace。

建议事件字段：

- `traceId`
- `stage`
- `component`
- `status`
- `startedAt`
- `endedAt`
- `durationMs`
- `sourceFile`
- `sourceClass`
- `sourceMethod`
- `details`

## 当前实施策略

- 第一阶段先做前端骨架和假数据驱动页面
- 第二阶段由 Spring Boot 返回真实查询结果、trace 和历史记录
- 第三阶段把 trace 与执行链路组件绑定为可观测性能力

## 当前进展

- 前端已经优先调用 `POST /api/query/execute`
- 已支持 `GET /api/query/history` 和 `GET /api/query/trace/{traceId}`
- 当前接口返回真实 SQL 执行结果、阶段级 trace 与原始文本结果
- 当前前端在后端不可用时会回退到 mock trace，便于独立开发
- 当前前端通过 `VITE_API_BASE_URL` 配置后端地址，已按前后端分离方式联调
- 当前前端表格区域已经消费后端结构化 `rows / columns`，不再依赖 `rawResult` 文本解析
- 当前后端 traceSteps 已开始由真实运行时埋点生成，而不是只靠 `QueryTraceService` 做阶段推断
