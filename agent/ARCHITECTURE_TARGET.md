# 目标架构

## 当前决策

当前阶段采用以下策略：

- 单仓库保留
- 先拆代码目录和职责边界
- 暂时不引入多个 Maven 模块
- 等边界稳定后，再评估是否拆成多模块

这意味着当前重点是“逻辑分层”，不是“构建系统复杂化”。

## 为什么现在不立刻拆多个 Maven

原因如下：

- 目前项目仍处于重构早期，边界还在变化
- 先拆多模块会放大依赖管理和测试组织复杂度
- 如果在职责未稳定时先拆模块，容易把混乱状态固化到多个模块中

因此，当前合理策略是：

1. 先明确职责边界
2. 先迁移包结构
3. 先稳定启动模型
4. 再决定是否做 Maven 多模块

## 第一阶段目标目录

当前推荐演进方向如下：

```text
src/main/java/com/indolyn/rill/
  app/
    boot/

  access/
    cli/
    gui/
    protocol/

  tools/

  core/
    catalog/
    common/
    compiler/
    engine/
    executor/
    storage/
    transaction/
```

## 各层职责

### app

Spring Boot 适配层。

职责：

- 应用启动
- Web UI 承载
- 后端服务层
- 后续 controller / service / config 等应用层能力

### access

外部访问层。

职责：

- CLI 终端入口
- GUI 桌面入口
- 协议兼容入口

### tools

辅助工具层。

职责：

- 数据导出
- 日志查看
- 手工演示工具
- 调试工具

### core

数据库内核主包。

- `core.catalog`
- `core.common`
- `core.compiler`
- `core.engine`
- `core.executor`
- `core.storage`
- `core.transaction`
- `core.DatabaseManager`

这些是数据库本体。当前这一步已经完成第一阶段迁移，后续重点不再是“是否迁入 core”，而是“如何保证 core 不被外层反向污染”。

当前已完成的关键净化动作：

- `Session` 已下沉到 `core.session`
- `core` 不再直接依赖 `access` / `tools` / `app`
- `app` 已建立最小 service / web 骨架，可作为未来 `rill-app` 模块前身

## 第二阶段可能演进

当以下条件满足后，再考虑多 Maven 模块：

- Spring Boot 层不再污染数据库内核
- access 和 tools 对内核依赖路径稳定
- 启动方式已经统一
- 测试和构建边界已经清晰
- app 层已经形成比较稳定的 service / web 适配层

届时可考虑：

- `rill-core`
- `rill-app`
- `rill-access`
- `rill-tools`

或者更保守地先拆成：

- `rill-core`
- `rill-app`

## 当前结论

当前结论非常明确：

- 代码目录现在应该拆
- 现在已经开始接近可拆模块状态，但仍建议先继续稳定 `app` 适配层
- 下一步如果拆，优先拆成两个模块：`rill-core` 和 `rill-app`

先把目录、包结构、职责边界拆清楚，才值得继续做物理模块化。
