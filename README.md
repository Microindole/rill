# Rill

Rill 是一个基于 Java 21 的数据库实验项目，当前仓库包含数据库内核、原生服务端、CLI/GUI 客户端、Spring Boot Web 后端和前端控制台。

## 模块

- `rill-core`：数据库内核
- `rill-server`：原生 TCP 服务端和 MySQL 协议兼容服务端
- `rill-client`：终端 SQL 客户端和本地 GUI 客户端
- `rill-app-web`：Spring Boot Web 后端
- `rill-launcher`：开发期工具入口，负责 `log / data` 等模式
- `web`：前端控制台

## 环境要求

- JDK 21
- Maven Wrapper
- Node.js 22
- PostgreSQL

如果机器上有多个 JDK，建议显式设置 `JAVA21_HOME`。

Windows:

```bat
set JAVA21_HOME=D:\Java\jdk-21
```

macOS / Linux:

```sh
export JAVA21_HOME=/path/to/jdk-21
```

## 构建

在仓库根目录执行。

Windows:

```bat
scripts\build.cmd
```

macOS / Linux:

```sh
./scripts/build.sh
```

等价命令：

Windows:

```bat
mvnw.cmd -DskipTests package
```

macOS / Linux:

```sh
./mvnw -DskipTests package
```

构建后主要产物位于：

```text
rill-server/target/rill-server-0.0.1-SNAPSHOT-server.jar
rill-server/target/rill-server-0.0.1-SNAPSHOT-mysql-server.jar
rill-client/target/rill-client-0.0.1-SNAPSHOT-cli.jar
rill-client/target/rill-client-0.0.1-SNAPSHOT-gui.jar
rill-app-web/target/rill-app-web-0.0.1-SNAPSHOT.jar
rill-launcher/target/rill-launcher-0.0.1-SNAPSHOT.jar
```

## 本地配置

本地敏感配置放在：

- [`config/rill-app-secrets.properties`](D:/works/rill/config/rill-app-secrets.properties)

参考模板：

- [`config/rill-app-secrets.example.properties`](D:/works/rill/config/rill-app-secrets.example.properties)

这个文件已被忽略，不会进入版本库。至少需要补上：

```properties
APP_DB_URL=jdbc:postgresql://localhost:5432/rill_app
APP_DB_USERNAME=postgres
APP_DB_PASSWORD=your-password
APP_AUTH_JWT_SECRET=dev-secret
APP_AUTH_FRONTEND_BASE_URL=http://localhost:5173
APP_WEB_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## 运行方式

仓库根目录已经提供统一入口：

- Windows：[`scripts/rill.cmd`](D:/works/rill/scripts/rill.cmd)
- macOS / Linux：[`scripts/rill.sh`](D:/works/rill/scripts/rill.sh)

这些脚本直接运行**已经打包好的 jar**。所以开发期通常是：

1. 先 `package`
2. 再用 `scripts/rill.*` 启动对应模式

### 常用命令

Windows:

```bat
scripts\rill.cmd help
scripts\rill.cmd server
scripts\rill.cmd mysql-server
scripts\rill.cmd sql
scripts\rill.cmd gui
scripts\rill.cmd log
scripts\rill.cmd data
scripts\rill.cmd web
```

macOS / Linux:

```sh
./scripts/rill.sh help
./scripts/rill.sh server
./scripts/rill.sh mysql-server
./scripts/rill.sh sql
./scripts/rill.sh gui
./scripts/rill.sh log
./scripts/rill.sh data
./scripts/rill.sh web
```

说明：

- `sql`：终端 CLI 客户端，之前的 CLI 入口仍然保留在这里
- `web`：启动 `rill-app-web` 打好的 Spring Boot jar
- `log / data`：通过 `rill-launcher` 提供的开发工具入口

## 本地开发

### 后端

推荐按和服务器一致的方式运行：

Windows:

```bat
mvnw.cmd -pl rill-app-web -am package
scripts\rill.cmd web
```

macOS / Linux:

```sh
./mvnw -pl rill-app-web -am package
./scripts/rill.sh web
```

健康检查：

```text
http://127.0.0.1:8080/api/health
```

### 前端

前端在 `web/` 目录下单独运行。

Windows / macOS / Linux:

```sh
cd web
npm install
npm run dev
```

本地联调时建议在 `web/.env.local` 中配置：

```properties
VITE_API_BASE_URL=http://127.0.0.1:8080
```

默认开发地址：

```text
http://localhost:5173
```

### 一套本地联调顺序

1. 启动 PostgreSQL
2. 在仓库根目录执行 `mvnw -pl rill-app-web -am package`
3. 在仓库根目录执行 `scripts/rill.* web`
4. 在 `web/` 目录执行 `npm run dev`
5. 打开前端页面联调

## 自动部署

当前 `main` 分支已接入自动部署：

- workflow：`.github/workflows/deploy-main.yml`
- 触发条件：`push` 到 `main` 或手动触发
- 部署方式：GitHub Actions 通过 SSH 登录服务器并执行 `/home/indolyn/deploy-rill.sh`

服务器侧要求：

- 项目部署在 `/home/indolyn/rill`
- 本地配置文件位于 `/home/indolyn/rill/config/rill-app-secrets.properties`
- 后端由 `systemd` 管理

部署脚本当前流程：

```sh
git fetch
git reset --hard origin/main
bash ./mvnw -pl rill-app-web -am package
sudo systemctl restart rill-app-web
sudo systemctl status rill-app-web --no-pager
```

## 其他

- CI、发布和平台打包仍然保留在 `.github/workflows/` 与 `packaging/` 中
- 如果只想看开发入口，优先看本 README 和 `scripts/` 目录
