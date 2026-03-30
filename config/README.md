`rill-app-secrets.properties` 用来放本地或部署环境的敏感配置。

使用方式：

1. 直接编辑同目录下的 `rill-app-secrets.properties`
2. 按实际环境填写数据库、CORS、JWT、邮件、验证码等参数
3. `rill-app-secrets.example.properties` 保留为参考模板

说明：

- `rill-app-secrets.properties` 已被同目录下的 `.gitignore` 忽略，不会进入版本库
- `application.properties` 已通过 `spring.config.import` 自动加载该文件
- `src/main/resources/application.properties` 中保留的是配置键和默认值，不是第二份真实密钥
- 没有配置时会退回到开发默认值，但生产环境不要使用默认值

