package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowDatabasesStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class QueryProcessor {

    private final DiskManager diskManager;
    @Getter
    private final BufferPoolManager bufferPoolManager;
    @Getter
    private final Catalog catalog;
    private final ExecutionEngine executionEngine;
    @Getter
    private final LogService logManager;
    @Getter
    private final LockService lockManager;
    @Getter
    private final TransactionManager transactionManager;
    @Getter
    private final DatabaseManager dbManager;
    private final QueryCompiler queryCompiler;
    private final QueryResultRenderer queryResultRenderer;
    private final BuiltInCommandHandler builtInCommandHandler;
    private final StatementTableNameResolver statementTableNameResolver;

    public QueryProcessor(String dbName) {
        try {
            QueryRuntime runtime = new QueryRuntime(dbName);
            this.dbManager = runtime.getDbManager();
            this.diskManager = runtime.getDiskManager();
            this.bufferPoolManager = runtime.getBufferPoolManager();
            this.catalog = runtime.getCatalog();
            this.logManager = runtime.getLogManager();
            this.lockManager = runtime.getLockManager();
            this.transactionManager = runtime.getTransactionManager();
            this.executionEngine = runtime.getExecutionEngine();
            this.queryCompiler = new QueryCompiler(catalog, runtime.getPlanner());
            this.queryResultRenderer = new QueryResultRenderer();
            this.builtInCommandHandler = new BuiltInCommandHandler(bufferPoolManager);
            this.statementTableNameResolver = new StatementTableNameResolver();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize database engine for " + dbName, e);
        }
    }

    public void close() throws IOException {
        bufferPoolManager.flushAllPages();
        logManager.flush();
        diskManager.close();
        logManager.close();
    }

    public String executeAndGetResult(String sql, Session session) {
        QueryResult queryResult = executeStructured(sql, session);
        if (!queryResult.success()) {
            return queryResult.message();
        }
        return queryResultRenderer.render(queryResult);
    }

    public QueryResult executeStructured(String sql, Session session) {
        Transaction txn = null;
        try {
            String builtInCommandResult = builtInCommandHandler.tryHandle(sql);
            if (builtInCommandResult != null) {
                return QueryResult.newSuccessResult(builtInCommandResult);
            }

            StatementNode ast = queryCompiler.parse(sql);
            if (ast == null) {
                return QueryResult.newSuccessResult("Empty statement.");
            }

            if (ast instanceof UseDatabaseStatementNode useDatabaseStatementNode) {
                String targetDbName = useDatabaseStatementNode.databaseName().getName();
                if (!dbManager.listDatabases().contains(targetDbName)) {
                    return QueryResult.newErrorResult("ERROR: Database '" + targetDbName + "' does not exist.");
                }
                session.setCurrentDatabase(targetDbName);
                queryCompiler.compileSystemStatement(ast);
                return QueryResult.newSuccessResult("Database changed to '" + targetDbName + "'.");
            }

            if (ast instanceof CreateDatabaseStatementNode
                || ast instanceof ShowDatabasesStatementNode
                || ast instanceof DropDatabaseStatementNode) {
                PlanNode plan = queryCompiler.compileSystemStatement(ast);
                TupleIterator executor = executionEngine.execute(plan, null);
                return collectQueryResult(executor);
            }

            txn = transactionManager.begin();
            System.out.println("Executing: " + sql + " in TxnID=" + txn.getTransactionId());

            PlanNode plan = queryCompiler.compile(ast, session).plan();
            TupleIterator executor = executionEngine.execute(plan, txn);

            QueryResult result = collectQueryResult(executor);
            transactionManager.commit(txn);
            return result;
        } catch (Exception e) {
            if (txn != null && txn.getState() == Transaction.State.ACTIVE) {
                try {
                    System.err.println("Error occurred, aborting transaction " + txn.getTransactionId());
                    transactionManager.abort(txn);
                } catch (IOException ioException) {
                    System.err.println("Failed to abort transaction: " + ioException.getMessage());
                }
            }
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("Error details: " + sw.toString());
            return QueryResult.newErrorResult("ERROR: " + e.getMessage());
        }
    }

    public String executeAndGetResult(String sql) {
        return executeAndGetResult(sql, Session.createAuthenticatedSession(-1, "root"));
    }

    public TupleIterator executeMysql(String sql, Session session) throws Exception {
        Transaction txn = transactionManager.begin();
        try {
            String normalizedSql = normalizeSqlForMysql(sql);
            CompiledStatement compiledStatement = queryCompiler.compile(normalizedSql, session);
            if (compiledStatement == null) {
                transactionManager.abort(txn);
                return null;
            }
            TupleIterator iterator = executionEngine.execute(compiledStatement.plan(), txn);
            transactionManager.commit(txn);
            return iterator;
        } catch (Exception e) {
            transactionManager.abort(txn);
            throw e;
        }
    }

    private String normalizeSqlForMysql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        String trimmedSql = sql.trim();

        if (!trimmedSql.endsWith(";")) {
            trimmedSql += ";";
        }

        return trimmedSql;
    }

    public TupleIterator createExecutorForQuery(String sql, Transaction txn, Session session)
        throws Exception {
        String normalizedSql = normalizeSqlForMysql(sql);

        System.out.println("[DEBUG] Creating executor for: " + normalizedSql);

        StatementNode ast = queryCompiler.parse(normalizedSql);
        if (ast == null) {
            return null;
        }

        System.out.println("[DEBUG] AST type: " + ast.getClass().getSimpleName());

        if (ast instanceof CreateDatabaseStatementNode || ast instanceof ShowDatabasesStatementNode) {
            PlanNode plan = queryCompiler.compileSystemStatement(ast);
            return executionEngine.execute(plan, null);
        }

        return executionEngine.execute(queryCompiler.compile(ast, session).plan(), txn);
    }

    public void execute(String sql) {
        String result = executeAndGetResult(sql);
        System.out.println(result);
    }

    public String render(QueryResult queryResult) {
        return queryResultRenderer.render(queryResult);
    }

    public String getTableNameFromAst(StatementNode ast) {
        return statementTableNameResolver.resolve(ast);
    }

    private QueryResult collectQueryResult(TupleIterator iterator) throws IOException {
        if (iterator == null) {
            return QueryResult.newSuccessResult("Query OK.");
        }

        List<com.indolyn.rill.core.model.Tuple> tuples = new ArrayList<>();
        while (iterator.hasNext()) {
            com.indolyn.rill.core.model.Tuple tuple = iterator.next();
            if (tuple != null) {
                tuples.add(tuple);
            }
        }

        if (iterator.getOutputSchema() == null) {
            return QueryResult.newSuccessResult("Query OK.");
        }

        List<String> columnNames = iterator.getOutputSchema().getColumnNames();
        if (!columnNames.isEmpty() && columnNames.get(0).endsWith("_rows")) {
            if (tuples.isEmpty() || tuples.get(0).getValues().isEmpty()) {
                return QueryResult.newSuccessResult("Query OK, 0 rows affected.");
            }
            int affectedRows = (Integer) tuples.get(0).getValues().get(0).getValue();
            return QueryResult.newSuccessResult("Query OK, " + affectedRows + " rows affected.");
        }

        return QueryResult.newSelectResult(iterator.getOutputSchema(), tuples);
    }
}
