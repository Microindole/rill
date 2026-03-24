package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.*;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LimitClauseNode;
import com.indolyn.rill.core.sql.ast.expression.OrderByClauseNode;

import java.util.List;

/**
 * @param selectList  查询的列或表达式列表
 * @param fromTable   查询的表
 * @param whereClause WHERE 条件子句 (可以为 null)
 * @param isSelectAll 是否为 SELECT *
 * @author hidyouth
 * @description: 表示一个 SELECT 语句
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
    implements StatementNode {
}

