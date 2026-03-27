# 运行模块

## 关注范围

本模块记录项目的启动方式、入口设计、运行模式与环境要求。

## 当前入口清单

当前代码中的主要 `main` 入口包括：

- `com.indolyn.rill.app.boot.RillLauncher`
- `com.indolyn.rill.app.boot.RillApplication`
- `com.indolyn.rill.access.protocol.ServerHost`
- `com.indolyn.rill.access.protocol.ServerRemote`
- `com.indolyn.rill.access.cli.InteractiveShell`
- `com.indolyn.rill.access.gui.AdvancedShell`
- `com.indolyn.rill.tools.DataReader`
- `com.indolyn.rill.tools.LogReader`

## 目标外部入口分类

后续对外运行方式统一收敛为四类：

1. CLI
2. GUI
3. Web UI
4. Navicat 兼容入口

这四类是面向用户或演示环境的正式访问方式，不再允许无限增长新的独立入口。

## 四类入口定义

### 1. CLI

职责：

- 终端交互
- 开发调试
- 脚本化调用
- 本地问题排查

当前对应：

- `InteractiveShell`
- 部分工具能力未来也可迁移为 CLI 子命令

规划方向：

- 统一收口到 `RillLauncher client` 或更明确的 CLI 子命令体系
- 后续应支持非交互参数模式
- 工具型能力尽量并入统一 CLI，而不是继续增加独立 `main`

### 2. GUI

职责：

- 本地桌面交互
- 可视化调试
- 本地演示

当前对应：

- `AdvancedShell`
- `LogReader`
- `DataReader` 的一部分交互能力

规划方向：

- 统一收口到 `RillLauncher gui` 下的不同子模式
- 区分“数据库操作 GUI”和“工具型 GUI”
- GUI 不作为部署主路径，只作为本地能力保留

### 3. Web UI

职责：

- 部署后展示
- 在线演示
- 面向浏览器访问

当前对应：

- 当前尚未正式建立
- 后续由 Spring Boot 承载

规划方向：

- Spring Boot 主要转向承载 Web UI 与服务化运行
- 现有 `RillApplication` 需要去掉演示副作用后再纳入正式体系
- Web UI 与后端接口的边界要清晰设计

### 4. Navicat 兼容入口

职责：

- 与外部数据库客户端工具对接
- 提供更接近真实数据库服务端的接入方式

当前对应：

- `ServerRemote`

规划方向：

- 作为独立服务端模式长期保留
- 后续需要明确协议兼容边界和运行参数
- 该入口应通过统一 launcher 进入，而不是长期直接暴露裸 `main`

## 当前入口归类

### 建议保留并收口到统一 launcher

- `RillLauncher`
- `ServerHost`
- `ServerRemote`
- `InteractiveShell`
- `AdvancedShell`
- `LogReader`
- `DataReader`

说明：

这些入口不是全部都作为顶层 `main` 长期存在，而是代表这些能力方向需要保留。

### 建议改造或降级

- `RillApplication`

说明：

`RillApplication` 已经去除了自动 demo 副作用。后续应继续改造成 Web UI / Spring Boot 正式承载入口，而不是实验入口。

## 当前保留策略

## 当前结构决策

- 当前已切到父 `pom` 聚合的多模块结构
- `rill-core` 负责数据库内核
- `rill-server` 负责原生服务端与 MySQL/Navicat 兼容服务端
- `rill-client` 负责 CLI、GUI 与本地工具入口
- `rill-app-web` 负责 Spring Boot Web 壳
- `rill-launcher` 只保留 IDE / 本地开发期统一入口

## 当前构建与启动约定

- `mvnw.cmd`、`mvnw` 已恢复为可用的统一构建入口
- `scripts/build.cmd`、`scripts/build.sh` 统一负责打包
- `scripts/rill.cmd`、`scripts/rill.sh` 统一负责按模式选择对应模块产物运行
- 如果定义了 `JAVA21_HOME`，wrapper 与脚本都会优先使用该 JDK
- 如果没有定义 `JAVA21_HOME`，则回退到当前 `JAVA_HOME / PATH`

Windows 推荐：

```bat
set JAVA21_HOME=D:\Java
scripts\build.cmd
scripts\rill.cmd help
```

macOS / Linux 推荐：

```sh
export JAVA21_HOME=/path/to/jdk-21
./scripts/build.sh
./scripts/rill.sh help
```

### 统一入口

- `RillLauncher` 作为本地开发期统一入口保留；正式分发以模块化产物为准

### 服务端能力

- `ServerHost`：原生 rill 服务端能力，短期保留，后续收口为 launcher 子命令
- `ServerRemote`：Navicat 兼容方向的服务端能力，长期保留，后续收口为 launcher 子命令

### 客户端能力

- `InteractiveShell`：CLI 能力保留
- `AdvancedShell`：GUI 能力保留

### 工具能力

- `DataReader`：工具能力保留，但不应长期维持为孤立入口
- `LogReader`：工具能力保留，但应归入 GUI 或工具子模式

### Spring Boot 能力

- `RillApplication`：保留，当前已去除自动 demo 污染，后续继续向 Web UI / 服务化承载入口演进
- `ShellRunner`：保留为手工演示工具，不再参与 Spring Boot 自动启动

## 当前废弃方向

以下做法应逐步废弃：

- 继续新增新的独立 `main` 入口
- 让演示代码挂在正式启动路径上
- 用 IDE 私有运行配置代替正式启动规范
- 让工具能力长期以分散入口存在

## 近期执行顺序

1. 定义 launcher 的稳定子命令体系
2. 明确哪些旧入口只保留兼容壳
3. 将工具能力逐步迁移到统一入口
4. 为 Web UI 和 Navicat 兼容入口预留清晰位置

## 文档同步规则

只要运行方式发生变化，必须同步更新：

- `agent/STATUS.md`
- `agent/ARCHITECTURE.md`
- `agent/modules/runtime.md`

