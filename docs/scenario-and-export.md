# 场景脚本与导出任务

## 场景脚本

场景用于保存一组可复现 SQL，适合演示初始化、回放和教学。

- 创建：`/api/workspace/scenarios`
- 运行：`/api/workspace/scenarios/{id}/run/{sessionId}`

执行时会按顺序逐条提交语句，并返回每条语句执行结果。

## 导出任务

导出任务用于把查询结果写入文件。

- 创建：`/api/workspace/export-tasks`
- 执行：`/api/workspace/export-tasks/{id}/run`
- 下载：`/api/workspace/export-tasks/{id}/download`

格式支持：

- `csv`
- `json`

默认导出目录：`target/exports`（可通过 `app.workspace.export-dir` 调整）。

