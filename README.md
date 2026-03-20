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

打包后会生成可直接运行的 fat jar:

```text
java -jar target/...jar help
```

## Run Modes

统一入口为 `com.indolyn.rill.RillLauncher`，因此 IDEA、VS Code、终端和部署环境都可以使用同一套启动方式。

Windows:

```bat
scripts\rill.cmd help
scripts\rill.cmd server --port=8848
scripts\rill.cmd client --host=127.0.0.1 --port=8848 --user=root
```

macOS / Linux:

```sh
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
