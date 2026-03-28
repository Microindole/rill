# Rill Core Testing

`rill-core` 的测试体系已按新分层重新起步，不再沿用历史目录。

当前重建原则：

- 由内到外：基础设施 -> 存储/事务 -> 编译器 -> 执行 -> 通信 -> 集成
- 由外到内：从真实 SQL / 真实请求 / 真实恢复场景反向压整条链路

当前第一批基线测试：

- `infrastructure.model`: 模型与序列化不变量
- `infrastructure.database`: 数据库目录与路径管理
- `infrastructure.transaction`: 日志记录编解码、日志文件读回、锁管理、事务状态转换
- `compiler`: 词法/语法最小 smoke
- `execution`: `QueryCompiler` 的 parse / compile 最小 smoke
- `integration`: 真实 SQL 执行、DML 变更、重启恢复最小 smoke

后续补测试时，优先遵守：

- 每个测试只锁一个边界
- 基础设施层测试必须小而快
- 慢测试、性能测试不直接回到默认 CI

当前已经落地的代表测试：

- `SchemaBaselineTest`
- `DatabaseManagerBaselineTest`
- `LogRecordBaselineTest`
- `LogManagerBaselineTest`
- `LockManagerBaselineTest`
- `TransactionManagerBaselineTest`
- `LexerParserBaselineTest`
- `QueryCompilerBaselineTest`
- `QueryProcessorSmokeTest`
- `DmlMutationSmokeTest`
- `RecoverySmokeTest`
