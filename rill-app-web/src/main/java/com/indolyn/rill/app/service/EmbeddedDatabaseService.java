package com.indolyn.rill.app.service;

import com.indolyn.rill.core.execution.QueryProcessor;
import com.indolyn.rill.core.execution.QueryResult;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.parser.Parser;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Default single-node database service backed directly by the embedded core engine.
 */
@Service
public class EmbeddedDatabaseService implements DatabaseService {

    private final QueryProcessorRegistry registry;

    public EmbeddedDatabaseService(QueryProcessorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public DatabaseExecution execute(String dbName, String sql) {
        String normalizedDbName = normalizeDbName(dbName);
        String normalizedSql = sql == null ? "" : sql.trim();
        String effectiveDbName = resolveEffectiveDbName(normalizedDbName, normalizedSql);
        boolean switchingDatabase = !effectiveDbName.equals(normalizedDbName);
        QueryProcessor processor = registry.getOrCreate(normalizedDbName);
        Session session = Session.createAuthenticatedSession(-1, "root");
        session.setCurrentDatabase(normalizedDbName);

        QueryResult queryResult = processor.executeStructured(normalizedSql, session);
        String rawResult = processor.render(queryResult);
        return new DatabaseExecution(
            queryResult.success()
                ? (switchingDatabase ? effectiveDbName : sessionDatabaseOrFallback(session, effectiveDbName))
                : normalizedDbName,
            normalizedSql,
            queryResult,
            rawResult);
    }

    @Override
    public List<String> getLoadedDatabases() {
        return registry.getLoadedDatabases();
    }

    private String normalizeDbName(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            return "default";
        }
        return dbName.trim();
    }

    private String resolveEffectiveDbName(String normalizedDbName, String normalizedSql) {
        if (normalizedSql.isEmpty() || !normalizedSql.regionMatches(true, 0, "use ", 0, 4)) {
            return normalizedDbName;
        }

        StatementNode statementNode = new Parser(new Lexer(normalizedSql).tokenize()).parse();
        if (statementNode instanceof UseDatabaseStatementNode useDatabaseStatementNode) {
            return useDatabaseStatementNode.databaseName().getName();
        }
        return normalizedDbName;
    }

    private String sessionDatabaseOrFallback(Session session, String fallback) {
        return session.getCurrentDatabase() == null || session.getCurrentDatabase().isBlank()
            ? fallback
            : session.getCurrentDatabase();
    }
}
