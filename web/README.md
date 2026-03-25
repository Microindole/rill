# Rill Web UI

## 技术栈

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- Tailwind CSS
- Vue Flow

## 当前状态

当前是第一阶段骨架：

- 已有可运行的 SQL Workbench 页面
- 已有查询结果区、执行流程图区、源码映射区
- 当前使用 mock trace 数据
- 尚未接入 Spring Boot 的真实查询接口

## 本地运行

```bash
npm install
npm run dev
```

默认会请求 `http://localhost:8080` 的后端 API。

如果后端地址不同，可以复制 `.env.example` 为 `.env.development` 或 `.env.production` 并修改：

```bash
VITE_API_BASE_URL=http://your-backend-host:port
```

## 生产构建

```bash
npm run build
```

## 下一步

- 接入 `app` 层真实 `query / trace / history / health` 接口
- 将 mock trace 替换为数据库执行链路真实数据
- 增加查询历史、错误面板和执行计划详情
