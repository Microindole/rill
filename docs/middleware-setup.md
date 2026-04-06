# Redis 与 RocketMQ 启动说明

本文档用于解决 `rill-app-web` 在本地开发和 Debian 部署时对 Redis 与 RocketMQ 的依赖问题。

## 当前结论

- Redis 与 RocketMQ 都是独立中间件，不会随 Spring Boot 一起安装。
- 当前项目已经接入：
  - Redis：短期认证状态、OAuth2 pending state 等
  - RocketMQ：`export_task` 异步导出
- 当前默认推荐：
  - 本地开发可以同时启动 Redis 与 RocketMQ
  - 生产环境如果机器只有 `2 核 2G`，Redis 可以同机部署，RocketMQ 只建议低负载演示使用

## 1. 本地 Win11 启动

推荐直接使用 Docker Desktop。

在仓库根目录执行：

```powershell
cd deploy/services
docker compose up -d
```

启动后会得到：

- Redis：`127.0.0.1:6379`
- RocketMQ NameServer：`127.0.0.1:9876`
- RocketMQ Broker：`127.0.0.1:10911`

本地开发的 `config/rill-app-secrets.properties` 建议至少包含：

```properties
APP_REDIS_HOST=127.0.0.1
APP_REDIS_PORT=6379
APP_REDIS_DATABASE=0

APP_EXPORT_TASK_TRANSPORT=rocketmq
APP_EXPORT_TASK_TOPIC=rill-export-task
APP_ROCKETMQ_NAME_SERVER=127.0.0.1:9876
```

如果你只是想先跑通应用，不想依赖 RocketMQ，把这一项保留为：

```properties
APP_EXPORT_TASK_TRANSPORT=local
```

## 2. Debian 服务器部署

同样推荐 Docker Compose。

```bash
cd /your/path/rill/deploy/services
docker compose up -d
```

### 部署前必须修改

编辑：

`deploy/services/rocketmq/broker.conf`

把：

```text
brokerIP1 = 127.0.0.1
```

改成 Debian 服务器可被客户端访问的公网 IP 或域名，例如：

```text
brokerIP1 = api.xxx.xxx
```

如果 RocketMQ 只在内网使用，也可以写内网 IP。

### Debian 2 核 2G 的建议

- Redis：可以同机部署
- RocketMQ：可以作为演示环境跑单节点，但不要按生产高负载预期使用
- 当前 Compose 已显式下调 JVM：
  - NameServer：`256m`
  - Broker：`512m`

这是为了让 RocketMQ 在小机器上更容易跑起来，但仍然属于低配演示方案。

## 3. Spring Boot 相关配置

当前 `rill-app-web` 已改成：

- `APP_EXPORT_TASK_TRANSPORT=local` 时，不依赖实际发送 MQ 消息
- `rocketmq.name-server` 默认回退到 `127.0.0.1:9876`

因此即使你没有马上配置 RocketMQ，也不会再因为 `rocketmq.name-server` 为空而直接启动失败。

## 4. 生产环境推荐配置

前端域名：`https://rill.indolyn.com`

后端域名：`https://api.xxx.xxx`

建议的 `config/rill-app-secrets.properties`：

```properties
APP_WEB_CORS_ALLOWED_ORIGINS=https://rill.indolyn.com
APP_AUTH_FRONTEND_BASE_URL=https://rill.indolyn.com

APP_REDIS_HOST=127.0.0.1
APP_REDIS_PORT=6379
APP_REDIS_DATABASE=0

APP_EXPORT_TASK_TRANSPORT=rocketmq
APP_EXPORT_TASK_TOPIC=rill-export-task
APP_ROCKETMQ_NAME_SERVER=127.0.0.1:9876
```

如果 RocketMQ 不打算在首版生产启用，把导出任务继续留在本地模式：

```properties
APP_EXPORT_TASK_TRANSPORT=local
```

## 5. 验证方式

### Redis

```bash
docker exec -it rill-redis redis-cli ping
```

返回 `PONG` 即正常。

### RocketMQ

查看容器日志：

```bash
docker logs rill-rocketmq-namesrv
docker logs rill-rocketmq-broker
```

如果日志中出现 NameServer / Broker 启动成功信息，即可继续联调应用。
