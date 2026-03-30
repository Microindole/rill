`rill-app-secrets.properties` 用来放本地或部署环境的敏感配置。

使用方式：

1. 复制同目录的 `rill-app-secrets.example.properties`
2. 重命名为 `rill-app-secrets.properties`
3. 按实际环境填写数据库、JWT、邮件、验证码等参数

说明：

- `rill-app-secrets.properties` 已被 `.gitignore` 忽略，不会进入版本库
- `application.properties` 已通过 `spring.config.import` 自动加载该文件
- 没有配置时会退回到开发默认值，但生产环境不要使用默认值
