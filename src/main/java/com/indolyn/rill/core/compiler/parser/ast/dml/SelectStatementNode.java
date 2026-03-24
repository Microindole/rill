package com.indolyn.rill.core.compiler.parser.ast.dml;

import com.indolyn.rill.core.compiler.parser.ast.*;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.LimitClauseNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.OrderByClauseNode;
import java.util.List;

/**
 * @author hidyouth
 * @description: 表示一个 SELECT 语句
 * @param selectList 查询的列或表达式列表
 * @param fromTable 查询的表
 * @param whereClause WHERE 条件子句 (可以为 null)
 * @param isSelectAll 是否为 SELECT *
 */
public record SelectStatementNode(
    List<ExpressionNode> selectList,
    IdentifierNode fromTable,
    IdentifierNode joinTable,
    ExpressionNode joinCondition,
    ExpressionNode whereClause,
    boolean isSelectAll,
    List<IdentifierNode> groupByClause,
    ExpressionNode havingClause, // having
    OrderByClauseNode orderByClause,
    LimitClauseNode limitClause)
    implements StatementNode {}
