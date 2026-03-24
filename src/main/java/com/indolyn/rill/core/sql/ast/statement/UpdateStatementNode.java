package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.SetClauseNode;

import java.util.List;

public record UpdateStatementNode(
    IdentifierNode tableName, List<SetClauseNode> setClauses, ExpressionNode whereClause)
    implements StatementNode {
}

