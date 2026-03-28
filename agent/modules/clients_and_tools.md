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

## 当前快照

- `DataReader / LogReader / ShellRunner` 已不再以独立 `main` 作为正式边界
- `log / data` 现在会按使用形态适配输出：CLI 走终端，GUI 走当前 GUI 内展示，发布包走统一 `rill` 命令
- `rill-server` 已开始补入口参数和协议辅助逻辑测试，不再完全裸奔

## 后续重点

- 区分正式运行入口与辅助工具入口
- 统一命令风格
- 逐步减少重复入口
- 明确哪些工具长期保留
- 继续补协议层 smoke 与客户端/工具的一致性测试
