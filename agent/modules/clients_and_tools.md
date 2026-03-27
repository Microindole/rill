# 客户端与工具模块

## 关注范围

本模块记录客户端、协议服务端与辅助工具。

主要组件：

- InteractiveShell
- AdvancedShell
- ServerHost
- ServerRemote
- DataReader
- LogReader

## 当前判断

这些组件能体现项目可操作性，当前已经开始按模块和产品边界收口：

- `rill-server` 负责原生服务端与 MySQL/Navicat 兼容服务端
- `rill-client` 负责 CLI、GUI 与本地工具入口
- 平台专用安装脚本位于 `packaging/windows|linux|macos/bin`

## 后续重点

- 区分正式运行入口与辅助工具入口
- 统一命令风格
- 逐步减少重复入口
- 明确哪些工具长期保留
