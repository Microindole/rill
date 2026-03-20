package com.indolyn.rill.core.compiler.parser.ast.dml;

import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.SetClauseNode;
import com.indolyn.rill.core.compiler.parser.ast.StatementNode;

import java.util.List;


public record UpdateStatementNode(
        IdentifierNode tableName,
        List<SetClauseNode> setClauses,
        ExpressionNode whereClause
) implements StatementNode {
}

