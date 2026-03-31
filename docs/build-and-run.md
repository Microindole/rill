# 构建产物与常用命令

## 构建产物位置

默认执行打包后，核心产物位于：

```text
rill-server/target/rill-server-0.0.1-SNAPSHOT-server.jar
rill-server/target/rill-server-0.0.1-SNAPSHOT-mysql-server.jar
rill-client/target/rill-client-0.0.1-SNAPSHOT-cli.jar
rill-client/target/rill-client-0.0.1-SNAPSHOT-gui.jar
rill-app-web/target/rill-app-web-0.0.1-SNAPSHOT.jar
rill-launcher/target/rill-launcher-0.0.1-SNAPSHOT.jar
```

## 常用命令

### Windows

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

### macOS / Linux

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

## 模式说明

- `server`: 原生 TCP 服务端
- `mysql-server`: MySQL 协议服务端
- `sql`: 终端 CLI 客户端
- `gui`: 本地 GUI 客户端
- `web`: Spring Boot Web 后端
- `log / data`: `rill-launcher` 工具模式

