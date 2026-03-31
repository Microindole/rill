# 部署与运维说明

## 自动部署

- Workflow：`.github/workflows/deploy-main.yml`
- 触发：`main` 分支 push / 手动触发
- 方式：GitHub Actions SSH 到服务器执行部署脚本

## 部署前配置（必须）

部署前必须在 `config/` 目录生成并填写：

- `config/rill-app-secrets.properties`

推荐从模板复制：

```sh
cp config/rill-app-secrets.example.properties config/rill-app-secrets.properties
```

至少替换以下关键项：

```properties
APP_DB_URL=jdbc:postgresql://localhost:5432/rill_app
APP_DB_USERNAME=postgres
APP_DB_PASSWORD=your-password
APP_AUTH_JWT_SECRET=replace-with-strong-secret
APP_AUTH_FRONTEND_BASE_URL=http://localhost:5173
APP_WEB_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

生产环境请使用实际域名与强随机密钥，避免使用示例值。

## 服务器约定

- 项目目录：`/home/indolyn/rill`
- 配置文件：`/home/indolyn/rill/config/rill-app-secrets.properties`
- 进程管理：`systemd`（`rill-app-web`）

## 最小检查项

1. `java -version` 为 JDK 21
2. 数据库连通性正常
3. `mvnw -pl rill-app-web -am package` 成功
4. `systemctl status rill-app-web` 正常
5. `GET /api/health` 返回健康状态
