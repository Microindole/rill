package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.expression.AggregateExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.AggregatePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.IndexScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.JoinPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.LimitPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.ProjectPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SortPlanNode;

import java.util.ArrayList;
import java.util.List;

class SelectPlanBuilder {

    private final Catalog catalog;

    SelectPlanBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    PlanNode build(SelectStatementNode ast) {
        PlanNode plan = buildScanPlan(ast);
        plan = appendJoin(ast, plan);
        plan = appendAggregation(ast, plan);
        plan = appendProjection(ast, plan);
        plan = appendOrderBy(ast, plan);
        return appendLimit(ast, plan);
    }

    private PlanNode buildScanPlan(SelectStatementNode ast) {
        TableInfo fromTableInfo = catalog.getTable(ast.fromTable().getName());
        IndexInfo indexInfo = findIndexForPredicate(fromTableInfo.getTableName(), ast.whereClause());

        if (indexInfo != null) {
            System.out.println(
                "[Planner] Index found for '"
                    + fromTableInfo.getTableName()
                    + "."
                    + indexInfo.getColumnName()
                    + "'. Using Index Scan.");
            Value searchValue = extractValueFromPredicate(ast.whereClause());
            return new IndexScanPlanNode(fromTableInfo, indexInfo, searchValue);
        }

        System.out.println("[Planner] No suitable index found for query. Using Sequential Scan.");
        return new SeqScanPlanNode(fromTableInfo, ast.whereClause());
    }

    private PlanNode appendJoin(SelectStatementNode ast, PlanNode plan) {
        if (ast.joinTable() == null) {
            return plan;
        }

        TableInfo rightTableInfo = catalog.getTable(ast.joinTable().getName());
        PlanNode rightPlan = new SeqScanPlanNode(rightTableInfo, null);
        return new JoinPlanNode(plan, rightPlan, ast.joinCondition());
    }

    private PlanNode appendAggregation(SelectStatementNode ast, PlanNode plan) {
        List<AggregateExpressionNode> allAggregates = collectAggregates(ast);
        boolean hasGroupBy = ast.groupByClause() != null && !ast.groupByClause().isEmpty();
        if (allAggregates.isEmpty() && !hasGroupBy) {
            return plan;
        }

        List<Column> intermediateColumns = new ArrayList<>();
        if (ast.groupByClause() != null) {
            for (IdentifierNode groupByCol : ast.groupByClause()) {
                intermediateColumns.add(findColumnInSchema(plan.getOutputSchema(), groupByCol.getName()));
            }
        }
        for (AggregateExpressionNode agg : allAggregates) {
            intermediateColumns.add(new Column(agg.toString(), DataType.INT));
        }

        Schema intermediateSchema = new Schema(intermediateColumns);
        return new AggregatePlanNode(
            plan, ast.groupByClause(), allAggregates, intermediateSchema, ast.havingClause());
    }

    private List<AggregateExpressionNode> collectAggregates(SelectStatementNode ast) {
        List<AggregateExpressionNode> selectAggregates =
            ast.selectList().stream()
                .filter(AggregateExpressionNode.class::isInstance)
                .map(AggregateExpressionNode.class::cast)
                .toList();

        List<AggregateExpressionNode> havingAggregates = new ArrayList<>();
        if (ast.havingClause() != null) {
            collectAggregates(ast.havingClause(), havingAggregates);
        }

        List<AggregateExpressionNode> allAggregates = new ArrayList<>(selectAggregates);
        for (AggregateExpressionNode havingAgg : havingAggregates) {
            if (allAggregates.stream().noneMatch(agg -> agg.toString().equals(havingAgg.toString()))) {
                allAggregates.add(havingAgg);
            }
        }
        return allAggregates;
    }

    private PlanNode appendProjection(SelectStatementNode ast, PlanNode plan) {
        if (ast.isSelectAll()) {
            return plan;
        }

        Schema inputForProjectionSchema = plan.getOutputSchema();
        List<Column> finalProjectedColumns = new ArrayList<>();
        for (ExpressionNode expr : ast.selectList()) {
            String colName = resolveProjectionName(expr);
            if (colName == null) {
                continue;
            }
            finalProjectedColumns.add(findColumnInSchema(inputForProjectionSchema, colName));
        }
        return new ProjectPlanNode(plan, new Schema(finalProjectedColumns));
    }

    private String resolveProjectionName(ExpressionNode expr) {
        if (expr instanceof IdentifierNode idNode) {
            return idNode.getName();
        }
        if (expr instanceof AggregateExpressionNode aggNode) {
            return aggNode.toString();
        }
        return null;
    }

    private PlanNode appendOrderBy(SelectStatementNode ast, PlanNode plan) {
        if (ast.orderByClause() == null) {
            return plan;
        }
        return new SortPlanNode(plan, ast.orderByClause());
    }

    private PlanNode appendLimit(SelectStatementNode ast, PlanNode plan) {
        if (ast.limitClause() == null) {
            return plan;
        }
        return new LimitPlanNode(plan, ast.limitClause().limit());
    }

    private void collectAggregates(ExpressionNode node, List<AggregateExpressionNode> list) {
        if (node instanceof AggregateExpressionNode aggNode) {
            list.add(aggNode);
        } else if (node instanceof BinaryExpressionNode binNode) {
            collectAggregates(binNode.left(), list);
            collectAggregates(binNode.right(), list);
        }
    }

    private Column findColumnInSchema(Schema schema, String columnName) {
        return schema.getColumns().stream()
            .filter(c -> c.getName().equalsIgnoreCase(columnName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Column '"
                            + columnName
                            + "' not found in plan's output schema. "
                            + "This should have been caught during semantic analysis."));
    }

    private IndexInfo findIndexForPredicate(String tableName, ExpressionNode predicate) {
        if (!(predicate instanceof BinaryExpressionNode binaryExpr)) {
            return null;
        }
        if (binaryExpr.operator().type() == TokenType.EQUAL
            && binaryExpr.left() instanceof IdentifierNode
            && binaryExpr.right() instanceof LiteralNode) {
            String columnName = ((IdentifierNode) binaryExpr.left()).getName();
            return catalog.getIndex(tableName, columnName);
        }
        return null;
    }

    private Value extractValueFromPredicate(ExpressionNode predicate) {
        if (predicate instanceof BinaryExpressionNode binaryExpr
            && binaryExpr.right() instanceof LiteralNode literal) {
            if (literal.literal().type() == TokenType.INTEGER_CONST) {
                return new Value(Integer.parseInt(literal.literal().lexeme()));
            }
            if (literal.literal().type() == TokenType.STRING_CONST) {
                return new Value(literal.literal().lexeme());
            }
        }
        throw new IllegalStateException(
            "Could not extract value from predicate for index scan. This indicates a planner bug.");
    }
}

