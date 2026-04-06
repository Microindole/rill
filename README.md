# Rill

Rill 是一个基于 Java 21 的数据库实验项目，当前仓库包含数据库内核、原生服务端、CLI/GUI 客户端、Spring Boot Web 后端和前端控制台。

[![CI](https://github.com/Microindole/rill/actions/workflows/ci.yml/badge.svg)](https://github.com/Microindole/rill/actions/workflows/ci.yml)
<!-- [![Core Coverage](https://github.com/Microindole/rill/actions/workflows/core-coverage.yml/badge.svg)](https://github.com/Microindole/rill/actions/workflows/core-coverage.yml) -->
[![codecov](https://codecov.io/gh/Microindole/rill/graph/badge.svg?flag=rill-core)](https://codecov.io/gh/Microindole/rill)
[![Java](https://img.shields.io/badge/JDK-21-007396?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Node.js](https://img.shields.io/badge/Node.js-22-339933?logo=node.js&logoColor=white)](https://nodejs.org/)
[![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

## 文档

- [文档导航](./docs/README.md)

## 模块

- `rill-core`：数据库内核
- `rill-server`：原生 TCP 服务端和 MySQL 协议兼容服务端
- `rill-client`：终端 SQL 客户端和本地 GUI 客户端
- `rill-app-web`：Spring Boot Web 后端
- `rill-launcher`：开发期工具入口，负责 `log / data` 等模式
- `web`：前端控制台

## 环境

- JDK 21
- Node.js 22
- Maven Wrapper
- PostgreSQL

如果机器上有多个 JDK，建议显式设置 `JAVA21_HOME`。

## 快速开始

1. 复制并填写配置文件：`config/rill-app-secrets.properties`（参考 `config/rill-app-secrets.example.properties`）
2. 在仓库根目录打包：
   - Windows: `scripts\build.cmd`
   - macOS / Linux: `./scripts/build.sh`
3. 启动 Web 后端：
   - Windows: `scripts\rill.cmd web`
   - macOS / Linux: `./scripts/rill.sh web`
4. 启动前端：
   - `cd web && npm install && npm run dev`

默认联调地址：

- 后端健康检查：`http://127.0.0.1:8080/api/health`
- 前端：`http://localhost:5173`

更多运行、架构、权限、部署细节见 [文档导航](./docs/README.md)。
常用命令和产物路径见 [构建产物与常用命令](./docs/build-and-run.md)。
Redis / RocketMQ 本地与 Debian 启动方式见 [中间件启动说明](./docs/middleware-setup.md)。
