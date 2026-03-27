# Agent 入口文件

本文件是所有 agent 进入本项目时必须优先读取的关键上下文。

## 使用规则

每次开始处理本项目时，先读取本文件，再按本文件给出的顺序读取其他文档。

推荐读取顺序：

1. `agent/ENTRY.md`
2. `agent/STATUS.md`
3. `agent/ARCHITECTURE.md`
4. `agent/ARCHITECTURE_TARGET.md`
5. `agent/WORKFLOW.md`
6. 按任务需要读取 `agent/modules/*.md`
7. 按任务需要读取 `agent/skills/*.md`
8. 如需背景和长期规划，再读取 `agent/foundation/*.md`

## 项目一句话定位

`rill` 是一个基于 Java 21 的自研数据库实验项目，具备 SQL 编译链路、存储引擎、B+Tree、事务/日志/恢复、CLI/GUI/协议入口，当前正在从“实训项目”重构为“适合 Java 面试展示的系统型项目”。

## 当前阶段

当前处于“内核第一轮重构与多模块/发布流水线收口已基本完成，重心转向编译器可扩展性、PostgreSQL 方言收口与后续分布式预留阶段”。

## 用户已确定的大流程

0. 修复启动入口，要跨平台，易部署
1. 重构整个项目底层代码，改用更合理的设计模式
2. 将 client 启动入口明确分为 CLI、GUI、Web UI、Navicat 兼容四个入口
3. 加入 Web 网页设计，并与 Spring Boot 结合
4. 引入网络编程、Redis 等，使用 submodule
5. 自动化测试、管理、CI/CD 工作流逐步完善，部署最后完成

## 当前优先目标

- 继续把编译器重构为更符合开闭原则的结构
- 以 PostgreSQL 方言为准补齐关键字、数据类型和核心语法
- 补齐编译链路回归测试，锁住当前编译器重构结果
- 再继续推进入口收口、系统层扩展和外层适配层整理

## 必读文档说明

- `agent/STATUS.md`：记录当前状态、最近变更、下一步工作、已知问题。每次改动后必须更新。
- `agent/ARCHITECTURE.md`：记录当前架构分层、关键运行链路、主要问题。发生架构变化时必须更新。
- `agent/ARCHITECTURE_TARGET.md`：记录当前多模块结构、职责边界，以及后续产品分发与分布式预留方向。
- `agent/WORKFLOW.md`：定义 agent 在本项目中的工作方式，尤其是“改代码后如何同步文档”。

## 模块文档说明

模块文档位于 `agent/modules/`，用于让 agent 在进入某个局部任务时快速理解对应子系统。

当前模块拆分：

- `runtime.md`：启动方式、入口、运行模式
- `app.md`：Spring Boot 适配层、后端服务壳、Web 入口
- `web.md`：Web UI 技术选型、页面结构、trace 可视化模型
- `compiler.md`：词法、语法、语义、规划
- `engine.md`：执行引擎、执行器、查询处理
- `system_layer.md`：系统层基础设施、存储/锁/日志/恢复/目录边界
- `storage.md`：磁盘、页、缓冲池、索引
- `transaction.md`：事务、锁、日志、恢复
- `clients_and_tools.md`：CLI、GUI、协议、辅助工具

## Skills 文档说明

`agent/skills/` 不是系统级 skill 机制本身，而是“本项目专用工作技能/操作规范”。

用于让 agent 在执行常见任务时有固定套路，例如：

- 启动入口收敛
- 文档维护
- 面试导向重构

## 文档维护刚性规则

任何 agent 只要修改了项目，都必须在结束前同步更新文档。

最低要求：

- 更新 `agent/STATUS.md`
- 如果改动影响架构或运行方式，更新 `agent/ARCHITECTURE.md`
- 如果改动影响某个模块，更新对应 `agent/modules/*.md`
- 如果形成新的稳定工作套路，更新对应 `agent/skills/*.md`

如果代码改了而文档没改，视为任务未完成。
