package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DeleteStatementNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowColumnsStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowCreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UpdateStatementNode;

class StatementTableNameResolver {

    String resolve(StatementNode ast) {
        if (ast instanceof SelectStatementNode selectNode) {
            return selectNode.fromTable().getName();
        }
        if (ast instanceof InsertStatementNode insertNode) {
            return insertNode.tableName().getName();
        }
        if (ast instanceof DeleteStatementNode deleteNode) {
            return deleteNode.tableName().getName();
        }
        if (ast instanceof UpdateStatementNode updateNode) {
            return updateNode.tableName().getName();
        }
        if (ast instanceof ShowColumnsStatementNode showColumnsNode) {
            return showColumnsNode.tableName().getName();
        }
        if (ast instanceof ShowCreateTableStatementNode showCreateNode) {
            return showCreateNode.tableName().getName();
        }
        if (ast instanceof CreateTableStatementNode createTableNode) {
            return createTableNode.tableName().getName();
        }
        return null;
    }
}
