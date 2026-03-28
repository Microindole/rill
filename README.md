# Rill

Rill 是一个基于 Java 21 与 Spring Boot 的数据库实验项目，当前主包名为 `com.indolyn.rill`。

## Maven Coordinates

```xml
<groupId>com.indolyn</groupId>
<artifactId>rill</artifactId>
```

## Build

Windows:

```bat
scripts\build.cmd
```

macOS / Linux:

```sh
./scripts/build.sh
```

也可以直接使用 Maven Wrapper:

```text
./mvnw -DskipTests package
mvnw.cmd -DskipTests package
```

如果本机同时存在多个 JDK，推荐显式提供 `JAVA21_HOME`，构建脚本和 Maven Wrapper 会优先使用它。

Windows:

```bat
set JAVA21_HOME=D:\Java
mvnw.cmd -DskipTests package
```

macOS / Linux:

```sh
export JAVA21_HOME=/path/to/jdk-21
./mvnw -DskipTests package
```

当前仓库已切到父 `pom` 聚合的多模块结构，默认 `package` 会分别在各模块下产出工件：

```text
rill-server/target/rill-server-0.0.1-SNAPSHOT-server.jar
rill-server/target/rill-server-0.0.1-SNAPSHOT-mysql-server.jar
rill-client/target/rill-client-0.0.1-SNAPSHOT-cli.jar
rill-client/target/rill-client-0.0.1-SNAPSHOT-gui.jar
rill-app-web/target/rill-app-web-0.0.1-SNAPSHOT.jar
rill-launcher/target/rill-launcher-0.0.1-SNAPSHOT.jar
```

模块职责：

- `rill-core`: 数据库内核
- `rill-server`: 原生 rill 服务端与 MySQL/Navicat 兼容服务端
- `rill-client`: CLI、GUI 与本地工具入口
- `rill-app-web`: Spring Boot Web 壳，内部依赖 `rill-core`，供 `web/` 前端调用
- `rill-launcher`: 本地开发期统一入口

## CI

仓库当前已接入 GitHub Actions CI：

- 后端跨平台构建：Java 21 + Maven 编译，覆盖 `Ubuntu x64 / Windows x64 / macOS ARM64 / Ubuntu ARM64 / Windows ARM64`
- 后端跨平台打包：多模块 `package`，并上传 `rill-server / rill-client / rill-app-web / rill-launcher` 工件
- 后端核心回归：`ParserTest / PlannerTest / SemanticAnalyzerTest / DataTypeTest / MysqlProtocolHandlerTest`
- 前端跨平台构建：Node 22 + `npm ci` + `npm run build`，覆盖 `Ubuntu x64 / Windows x64 / macOS ARM64 / Ubuntu ARM64 / Windows ARM64`

CI 默认会在 `push`、`pull_request` 和手动触发时运行，并根据变更路径跳过不相关的前后端 job。
当前 workflow 已按职责拆分为主编排文件和可复用子 workflow，避免单文件持续膨胀。

说明：

- 当前采用“双层 CI”：
  - 第一层：x64 + ARM64 的跨平台打包矩阵，优先发现脚本、路径、wrapper、shade/repackage 和平台兼容问题
  - 第二层：Linux 下的核心回归套件，控制 CI 成本同时保留行为校验
- 现在因为已经拆成 `rill-server / rill-client / rill-app-web / rill-launcher`，后续继续接 `jpackage` 或 Windows `exe` 打包时，CI 分工会更清楚
- 当前 ARM64 runner 使用 GitHub public 仓库可直接使用的标准 GitHub-hosted runner，不依赖 self-hosted 机器
- 当前没有直接使用全量 `./mvnw verify` 作为主 CI 命令，因为仓库里仍有一批历史测试尚未完全收口
- 现阶段先把稳定维护中的核心回归套件纳入正式 CI，后续再逐步扩大覆盖面

## Release

仓库当前已增加 tag 驱动的发布流水线。推送 `v*` tag 后，会自动生成并上传以下分层产物到 GitHub Release：

- Windows：`rill-core-cli-<tag>-windows-x64.zip`、`rill-desktop-<tag>-windows-x64.zip`、`rill-mysql-compat-<tag>-windows-x64.zip`
- Windows 安装器：`rill-<tag>-windows-x64-setup.exe`，固定安装 `core + cli`，可选安装 `gui + mysqlcompat`
- Linux：`rill-core-cli-<tag>-linux-x64.tar.gz`、`rill-desktop-<tag>-linux-x64.tar.gz`、`rill-mysql-compat-<tag>-linux-x64.tar.gz`
- macOS：`rill-core-cli-<tag>-macos-arm64.tar.gz`、`rill-desktop-<tag>-macos-arm64.tar.gz`、`rill-mysql-compat-<tag>-macos-arm64.tar.gz`
- Web API：`rill-web-api-<tag>.jar`，单独分发但内部仍携带 `rill-core` 依赖，不内嵌前端静态资源
- Web UI：`rill-web-ui-<tag>.jar`，先构建 `web/dist` 再打入 `rill-app-web` 的 `static/`，同样内含 `rill-core` 依赖

当前桌面/本地发布已按 edition 收口：

- `core-cli`：`server + sql + log + data`
- `desktop`：`core-cli + gui`
- `mysql-compat`：`core-cli + mysql-server`

平台专用安装脚本目录为：

- `packaging/windows/bin/*.cmd`
- `packaging/linux/bin/*.sh`
- `packaging/macos/bin/*.sh`

这些文件属于发布资产，会随各平台安装包一起分发，不应加入忽略规则。

## Dependabot

仓库当前已启用 Dependabot，覆盖：

- GitHub Actions
- 根目录 Maven 依赖
- `web/` 下的 npm 依赖

默认按每周节奏创建依赖更新 PR。

对于 `pull_request`，CI 还会额外执行依赖变更审查，避免高风险依赖更新直接进入主分支。

## Run Modes

`com.indolyn.rill.app.boot.RillLauncher` 仍保留为 IDE / 开发期统一入口；正式打包分发改为模块化工件。`server/mysql-server/sql/gui/log/data` 由各自模块独立打包，Spring Boot Web 壳单独位于 `rill-app-web`。

Windows:

```bat
set JAVA21_HOME=D:\Java
scripts\rill.cmd help
scripts\rill.cmd server --port=8848
scripts\rill.cmd mysql-server --port=9999
scripts\rill.cmd sql --host=127.0.0.1 --port=8848 --user=root
scripts\rill.cmd gui
scripts\rill.cmd log
scripts\rill.cmd data
scripts\rill.cmd web
```

macOS / Linux:

```sh
export JAVA21_HOME=/path/to/jdk-21
./scripts/rill.sh help
./scripts/rill.sh server --port=8848
./scripts/rill.sh mysql-server --port=9999
./scripts/rill.sh sql --host=127.0.0.1 --port=8848 --user=root
./scripts/rill.sh gui
./scripts/rill.sh log
./scripts/rill.sh data
./scripts/rill.sh web
```

也可以直接运行模块化产物：

```text
java -jar rill-server/target/rill-server-0.0.1-SNAPSHOT-server.jar --port=8848
java -jar rill-server/target/rill-server-0.0.1-SNAPSHOT-mysql-server.jar --port=9999
java -jar rill-client/target/rill-client-0.0.1-SNAPSHOT-cli.jar --host=127.0.0.1 --port=8848 --user=root
java -jar rill-client/target/rill-client-0.0.1-SNAPSHOT-gui.jar
java -jar rill-app-web/target/rill-app-web-0.0.1-SNAPSHOT.jar
```

## Environment Variables

服务端:

- `RILL_PORT`: 原生 rill 服务监听端口
- `RILL_MYSQL_PORT`: MySQL 协议服务监听端口

客户端:

- `RILL_HOST`: 默认服务地址
- `RILL_PORT`: 默认服务端口
- `RILL_USER`: 默认用户名

## Domain

项目域名归属使用 `indolyn.com`。

