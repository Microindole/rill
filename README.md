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

打包后会生成可直接运行的 fat jar:

```text
java -jar target/...jar help
```

## CI

仓库当前已接入 GitHub Actions CI：

- 后端：Java 21 + Maven 编译
- 后端核心回归：`ParserTest / PlannerTest / SemanticAnalyzerTest / DataTypeTest / MysqlProtocolHandlerTest`
- 前端：Node 22 + `npm ci` + `npm run build`

CI 默认会在 `push`、`pull_request` 和手动触发时运行，并根据变更路径跳过不相关的前后端 job。

说明：

- 当前没有直接使用全量 `./mvnw verify` 作为 CI 主命令，因为仓库里仍有一批历史测试尚未完全收口
- 现阶段先把稳定维护中的核心回归套件纳入正式 CI，后续再逐步扩大覆盖面

## Dependabot

仓库当前已启用 Dependabot，覆盖：

- GitHub Actions
- 根目录 Maven 依赖
- `web/` 下的 npm 依赖

默认按每周节奏创建依赖更新 PR。

对于 `pull_request`，CI 还会额外执行依赖变更审查，避免高风险依赖更新直接进入主分支。

## Run Modes

统一入口为 `com.indolyn.rill.RillLauncher`，因此 IDEA、VS Code、终端和部署环境都可以使用同一套启动方式。

Windows:

```bat
set JAVA21_HOME=D:\Java
scripts\rill.cmd help
scripts\rill.cmd server --port=8848
scripts\rill.cmd client --host=127.0.0.1 --port=8848 --user=root
```

macOS / Linux:

```sh
export JAVA21_HOME=/path/to/jdk-21
./scripts/rill.sh help
./scripts/rill.sh server --port=8848
./scripts/rill.sh client --host=127.0.0.1 --port=8848 --user=root
```

也可以直接运行 jar:

```text
java -jar target/rill-0.0.1-SNAPSHOT.jar server --port=8848
java -jar target/rill-0.0.1-SNAPSHOT.jar mysql-server --port=9999
java -jar target/rill-0.0.1-SNAPSHOT.jar client --host=127.0.0.1 --port=8848 --user=root
java -jar target/rill-0.0.1-SNAPSHOT.jar gui
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
