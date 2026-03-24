# 当前状态

## 当前阶段

阶段名称：总体规划已明确，正在进入“启动入口统一”阶段

## 已完成

- 初步梳理了项目整体结构和技术栈
- 确认项目使用 Java 21
- 确认并测试了多个现有入口的可启动性
- 增加了统一入口 `RillLauncher`
- 增加了跨平台脚本：`scripts/build.cmd`、`scripts/build.sh`、`scripts/rill.cmd`、`scripts/rill.sh`
- 建立了 `agent` 文档体系第一版
- 明确了项目后续的大流程规划
- 明确了运行入口的目标四分类：CLI / GUI / Web UI / Navicat 兼容
- 补充了运行模块的入口保留与收敛策略
- 修正了 `.gitignore` 中对 Maven Wrapper 的错误忽略规则
- 清理了 Spring Boot 启动副作用，保留了 Spring Boot 但移除了自动 demo 执行
- 完成了第一阶段目录迁移：入口层、Spring Boot 层、工具层开始按职责拆包
- 明确当前策略为“先拆目录，不立刻加多个 Maven 模块”
- 完成了 `core` 第一阶段迁移：数据库内核主包收敛到 `com.indolyn.rill.core.*`
- 清理了 `core` 对 `access` 的直接反向依赖，会话抽象已下沉到 `core.session`
- 建立了 `app` 最小骨架：service registry、query service、health controller
- 增加了 `.gitattributes`，统一跨平台换行策略
- 增加了 `.editorconfig`，统一基础缩进与换行规则
- 建立了 IntelliJ IDEA Java 代码风格基准文件
- 已按 IntelliJ IDEA 项目级代码风格对整仓 Java 文件完成一次重格式化
- 完成数据库内核第一批核心重构：`Planner`、`SemanticAnalyzer`、`ExecutionEngine` 已从巨型 `instanceof` 分发改为注册式处理结构

## 已确认事实

- 项目不是普通 CRUD，而是数据库内核实验项目
- 当前存在多个入口，后续需要收敛
- `ServerHost` 与 `ServerRemote` 都能启动
- `InteractiveShell` 能启动
- `AdvancedShell` 与 `LogReader` 属于 GUI 型入口
- `DataReader` 是交互式工具入口
- `RillApplication` 当前不适合直接作为正式启动入口
- `RillApplication` 现已不再自动执行 demo 逻辑
- `ShellRunner` 现为手工启动的演示工具，不再污染 Spring Boot 启动
- 当前已完成第一阶段包迁移：`app.boot`、`access.*`、`tools`
- 当前已完成数据库内核主包迁移：`catalog/common/compiler/engine/executor/storage/transaction/DatabaseManager` 已进入 `core`
- 当前 `core` 已不再直接依赖 `access`、`tools`、`app`
- 当前 `app` 已开始作为 Spring Boot 外层适配层调用 `core`
- 当前已开始统一仓库换行策略，减少 Windows 环境下的提交噪音
- 当前 Java 代码风格基准改为 IntelliJ IDEA 默认风格变体：4 空格缩进、`{` 行尾
- 当前主代码与测试代码都已按新的 IDEA 风格统一过一轮
- 当前编译器与执行器主分发点已开始遵守开闭原则，新增语句/计划类型不再必须改动单一超长方法
- 终端和 Maven 的 JDK 版本曾存在不一致，需要坚持 Java 21

## 用户已确定的总体规划

0. 修复启动入口，要跨平台，易部署
1. 重构整个项目底层代码，改用更合理的设计模式
2. 将 client 启动入口明确分为 CLI、GUI、Web UI、Navicat 兼容四个入口
3. 加入 Web 网页设计，并与 Spring Boot 结合
4. 引入网络编程、Redis 等，使用 submodule 组织更复杂能力
5. 自动化测试、管理与 CI/CD 工作流逐步完善，部署放在最后完成

## 当前主要待办

1. 继续重构数据库内核，优先拆解编译器与执行器内部职责
2. 将现有入口进一步收口到统一 launcher
3. 开始数据库内核与 Spring Boot 适配层的边界强化
4. 继续完善 `app` 层的 controller / service / dto 结构
5. 再评估是否拆成 `rill-core` / `rill-app` 两个 Maven 模块
6. 再进入测试补强与更深层的模式重构

## 最近一次变更

- 移除了会与 IDEA 默认风格冲突的 Maven Java formatter，并完成一次整仓 IDEA 风格重格式化
- 影响范围：Java 主代码、测试代码、IDE 项目配置、团队协作规范
- 当前结果：项目以 `code-style/intellij-java-style.xml` 和 `.idea/codeStyles` 作为 Java 风格基准，JDK 21 下 `package` 通过
- 下一步建议：后续提交尽量按“功能改动”和“纯格式化改动”分开，避免再次出现超大混合提交

## 当前建议顺序

1. 先修入口和启动模型
2. 再拆 demo 逻辑
3. 再把旧入口改成兼容层或移除
4. 再做底层架构重构
5. 再做 Web UI 和网络化扩展
6. 最后补齐测试、管理和 CI/CD

## 风险提示

- 如果在启动结构还没稳定前就大量加功能，项目会更难维护
- 如果继续保留重复入口，后续文档和运行方式会持续分裂
- 如果过早引入复杂外部组件，底层重构会被打断
- 如果不持续更新文档，agent 协作质量会快速下降

## 文档更新规则

每次代码变更完成后，至少更新本文件，说明：

- 改了什么
- 影响了哪些模块
- 当前结果
- 下一步建议
