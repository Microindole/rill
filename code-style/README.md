# 代码风格

Java 代码风格以 IntelliJ IDEA 默认风格为基准，并统一补充以下项目规则：

- 缩进：4 个空格
- 大括号：同行尾
- 行宽：120
- 禁止使用制表符

仓库内提供的 [`intellij-java-style.xml`](./intellij-java-style.xml) 可直接导入 IntelliJ IDEA：

1. `Settings`
2. `Editor`
3. `Code Style`
4. 右上角齿轮
5. `Import Scheme`
6. 选择本文件

当前项目不再通过 Maven 强制另一套 Java formatter，避免与 IDEA 默认格式冲突。
