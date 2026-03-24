package com.indolyn.rill.core.compiler.planner;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.common.model.Column;
import com.indolyn.rill.core.common.model.DataType;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.common.model.Tuple;
import com.indolyn.rill.core.common.model.Value;
import com.indolyn.rill.core.compiler.lexer.TokenType;
import com.indolyn.rill.core.compiler.parser.ast.*;
import com.indolyn.rill.core.compiler.parser.ast.dcl.CreateUserStatementNode;
import com.indolyn.rill.core.compiler.parser.ast.dcl.GrantStatementNode;
import com.indolyn.rill.core.compiler.parser.ast.ddl.*;
import com.indolyn.rill.core.compiler.parser.ast.dml.DeleteStatementNode;
import com.indolyn.rill.core.compiler.parser.ast.dml.InsertStatementNode;
import com.indolyn.rill.core.compiler.parser.ast.dml.SelectStatementNode;
import com.indolyn.rill.core.compiler.parser.ast.dml.UpdateStatementNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.*;
import com.indolyn.rill.core.compiler.parser.ast.misc.*;
import com.indolyn.rill.core.compiler.planner.plan.*;
import com.indolyn.rill.core.compiler.planner.plan.dcl.CreateUserPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.dcl.GrantPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.ddl.*;
import com.indolyn.rill.core.compiler.planner.plan.dml.DeletePlanNode;
import com.indolyn.rill.core.compiler.planner.plan.dml.InsertPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.dml.UpdatePlanNode;
import com.indolyn.rill.core.compiler.planner.plan.query.*;
import com.indolyn.rill.core.compiler.planner.plan.show.ShowColumnsPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.show.ShowCreateTablePlanNode;
import com.indolyn.rill.core.compiler.planner.plan.show.ShowDatabasesPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.show.ShowTablesPlanNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hidyouth
 * @description: 执行计划生成器 负责将经过语义分析的AST转换为物理执行计划
 */
public class Planner {

    private final Catalog catalog;
    private final Map<Class<? extends StatementNode>, StatementPlanner<? extends StatementNode>>
        statementPlanners = new LinkedHashMap<>();

    public Planner(Catalog catalog) {
        this.catalog = catalog;
        registerStatementPlanners();
    }

    public PlanNode createPlan(StatementNode ast) {
        StatementPlanner<StatementNode> planner = resolvePlanner(ast);
        if (planner != null) {
            return planner.createPlan(ast);
        }

        throw new UnsupportedOperationException(
            "Unsupported statement type for planning: " + ast.getClass().getSimpleName());
    }

    private void registerStatementPlanners() {
        registerPlanner(CreateTableStatementNode.class, this::createTablePlan);
        registerPlanner(
            CreateDatabaseStatementNode.class,
            stmt -> new CreateDatabasePlanNode(stmt.databaseName().getName()));
        registerPlanner(InsertStatementNode.class, this::createInsertPlan);
        registerPlanner(SelectStatementNode.class, this::createSelectPlan);
        registerPlanner(DeleteStatementNode.class, this::createDeletePlan);
        registerPlanner(UpdateStatementNode.class, this::createUpdatePlan);
        registerPlanner(DropTableStatementNode.class, this::createDropTablePlan);
        registerPlanner(
            DropDatabaseStatementNode.class,
            stmt -> new DropDatabasePlanNode(stmt.databaseName().getName()));
        registerPlanner(AlterTableStatementNode.class, this::createAlterTablePlan);
        registerPlanner(ShowTablesStatementNode.class, stmt -> new ShowTablesPlanNode());
        registerPlanner(
            ShowColumnsStatementNode.class,
            stmt -> new ShowColumnsPlanNode(stmt.tableName().getName()));
        registerPlanner(
            ShowCreateTableStatementNode.class,
            stmt -> new ShowCreateTablePlanNode(stmt.tableName().getName()));
        registerPlanner(ShowDatabasesStatementNode.class, stmt -> new ShowDatabasesPlanNode());
        registerPlanner(CreateIndexStatementNode.class, this::createIndexPlan);
        registerPlanner(
            CreateUserStatementNode.class,
            stmt -> new CreateUserPlanNode(stmt.username(), stmt.password()));
        registerPlanner(
            GrantStatementNode.class,
            stmt -> new GrantPlanNode(stmt.privileges(), stmt.tableName(), stmt.username()));
        registerPlanner(
            UseDatabaseStatementNode.class, stmt -> null); // USE语句不需要计划节点，在QueryProcessor中直接处理
    }

    private <T extends StatementNode> void registerPlanner(
        Class<T> statementType, StatementPlanner<T> planner) {
        statementPlanners.put(statementType, planner);
    }

    @SuppressWarnings("unchecked")
    private StatementPlanner<StatementNode> resolvePlanner(StatementNode ast) {
        StatementPlanner<? extends StatementNode> exactPlanner = statementPlanners.get(ast.getClass());
        if (exactPlanner != null) {
            return (StatementPlanner<StatementNode>) exactPlanner;
        }

        for (Map.Entry<Class<? extends StatementNode>, StatementPlanner<? extends StatementNode>>
            entry : statementPlanners.entrySet()) {
            if (entry.getKey().isInstance(ast)) {
                return (StatementPlanner<StatementNode>) entry.getValue();
            }
        }
        return null;
    }

    private PlanNode createTablePlan(CreateTableStatementNode ast) {
        String tableName = ast.tableName().getName();
        List<Column> columns =
            ast.columns().stream()
                .map(
                    colDef ->
                        new Column(
                            colDef.columnName().getName(),
                            DataType.valueOf(colDef.dataType().getName().toUpperCase())))
                .collect(Collectors.toList());
        Schema schema =
            new Schema(
                columns, ast.primaryKeyColumn() != null ? ast.primaryKeyColumn().getName() : null);
        return new CreateTablePlanNode(tableName, schema);
    }

    private PlanNode createInsertPlan(InsertStatementNode ast) {
        TableInfo tableInfo = catalog.getTable(ast.tableName().getName());
        Schema schema = tableInfo.getSchema();
        List<Value> values = new ArrayList<>();

        // --- 核心修改：根据 Schema 类型来解析字面量 ---
        for (int i = 0; i < ast.values().size(); i++) {
            ExpressionNode expr = ast.values().get(i);
            // 找到此位置对应的列定义
            String colName = ast.columns().get(i).getName();
            Column column = schema.getColumn(colName);
            DataType expectedType = column.getType();

            if (expr instanceof LiteralNode literal) {
                String lexeme = literal.literal().lexeme();
                TokenType tokenType = literal.literal().type();

                switch (expectedType) {
                    case INT:
                        values.add(new Value(Integer.parseInt(lexeme)));
                        break;
                    case VARCHAR:
                        values.add(new Value(lexeme));
                        break;
                    case DECIMAL:
                        values.add(new Value(new BigDecimal(lexeme)));
                        break;
                    case DATE:
                        values.add(new Value(LocalDate.parse(lexeme)));
                        break;
                    case BOOLEAN:
                        values.add(new Value(tokenType == TokenType.TRUE));
                        break;
                    case FLOAT: //
                        values.add(new Value(Float.parseFloat(lexeme)));
                        break;
                    case DOUBLE: //
                        values.add(new Value(Double.parseDouble(lexeme)));
                        break;
                    case CHAR: //
                        // 使用可以指定类型的构造函数
                        values.add(new Value(DataType.CHAR, lexeme));
                        break;
                    default:
                        throw new IllegalStateException(
                            "Unsupported data type in planner for INSERT: " + expectedType);
                }
            }
        }

        Tuple tuple = new Tuple(values);
        return new InsertPlanNode(tableInfo, List.of(tuple));
    }

    /**
     * 新增：为 CREATE INDEX 语句创建执行计划。
     *
     * @param ast CreateIndexStatementNode
     * @return CreateIndexPlanNode
     */
    private PlanNode createIndexPlan(CreateIndexStatementNode ast) {
        String indexName = ast.getIndexName().getName();
        String tableName = ast.getTableName().getName();
        // 假设AST中只有一个列用于索引
        String columnName = ast.getColumnNames().get(0).getName();

        TableInfo tableInfo = catalog.getTable(tableName);
        if (tableInfo == null) {
            throw new IllegalStateException("Table '" + tableName + "' not found for index creation.");
        }
        return new CreateIndexPlanNode(indexName, tableName, columnName, tableInfo);
    }

    /**
     * 【已修复版本】创建 SELECT 查询计划的逻辑
     */
    private PlanNode createSelectPlan(SelectStatementNode ast) {
        // 基础扫描层
        TableInfo fromTableInfo = catalog.getTable(ast.fromTable().getName());
        PlanNode plan;
        // 检查是否有 WHERE 子句，以及是否能找到合适的索引
        IndexInfo indexInfo = findIndexForPredicate(fromTableInfo.getTableName(), ast.whereClause());

        if (indexInfo != null) {
            // 优化器：发现可以使用索引，生成 IndexScan 计划
            System.out.println(
                "[Planner] Index found for '"
                    + fromTableInfo.getTableName()
                    + "."
                    + indexInfo.getColumnName()
                    + "'. Using Index Scan.");
            Value searchValue = extractValueFromPredicate(ast.whereClause());
            plan = new IndexScanPlanNode(fromTableInfo, indexInfo, searchValue);
        } else {
            // 优化器：没有找到合适的索引
            System.out.println("[Planner] No suitable index found for query. Using Sequential Scan.");
            // 将 WHERE 子句（如果存在）直接下推到 SeqScanPlanNode
            plan = new SeqScanPlanNode(fromTableInfo, ast.whereClause());
            // 注意：我们不再在这里创建 FilterPlanNode，因为过滤逻辑已经被下推了
        }
        // 3. JOIN 层
        if (ast.joinTable() != null) {
            TableInfo rightTableInfo = catalog.getTable(ast.joinTable().getName());
            PlanNode rightPlan = new SeqScanPlanNode(rightTableInfo, null);
            plan = new JoinPlanNode(plan, rightPlan, ast.joinCondition());
        }

        // 4. 聚合与 HAVING 过滤层
        // --- START OF FIX ---
        List<AggregateExpressionNode> selectAggregates =
            ast.selectList().stream()
                .filter(AggregateExpressionNode.class::isInstance)
                .map(AggregateExpressionNode.class::cast)
                .toList();

        List<AggregateExpressionNode> havingAggregates = new ArrayList<>();
        if (ast.havingClause() != null) {
            collectAggregates(ast.havingClause(), havingAggregates);
        }

        // 合并并去重所有需要计算的聚合函数
        List<AggregateExpressionNode> allAggregates = new ArrayList<>(selectAggregates);
        for (AggregateExpressionNode havingAgg : havingAggregates) {
            // 使用 toString() 作为唯一标识来判断是否已存在
            if (allAggregates.stream().noneMatch(agg -> agg.toString().equals(havingAgg.toString()))) {
                allAggregates.add(havingAgg);
            }
        }

        boolean hasGroupBy = ast.groupByClause() != null && !ast.groupByClause().isEmpty();
        if (!allAggregates.isEmpty() || hasGroupBy) {
            // 为聚合步骤构建中间 Schema，它包含所有分组列和所有聚合列
            List<Column> intermediateColumns = new ArrayList<>();
            if (ast.groupByClause() != null) {
                for (IdentifierNode groupByCol : ast.groupByClause()) {
                    intermediateColumns.add(findColumnInSchema(plan.getOutputSchema(), groupByCol.getName()));
                }
            }
            for (AggregateExpressionNode agg : allAggregates) {
                // 简化处理，所有聚合结果都为 INT
                intermediateColumns.add(new Column(agg.toString(), DataType.INT));
            }
            Schema intermediateSchema = new Schema(intermediateColumns);

            // 创建 AggregatePlanNode，传入所有聚合函数
            plan =
                new AggregatePlanNode(
                    plan, ast.groupByClause(), allAggregates, intermediateSchema, ast.havingClause());
        }
        // --- END OF FIX ---

        // 5. 最终投影层
        if (!ast.isSelectAll()) {
            Schema inputForProjectionSchema = plan.getOutputSchema();
            List<Column> finalProjectedColumns = new ArrayList<>();
            for (ExpressionNode expr : ast.selectList()) {
                String colName;
                if (expr instanceof IdentifierNode idNode) {
                    colName = idNode.getName();
                } else if (expr instanceof AggregateExpressionNode aggNode) {
                    colName = aggNode.toString();
                } else {
                    // Should be caught by semantic analysis
                    continue;
                }
                finalProjectedColumns.add(findColumnInSchema(inputForProjectionSchema, colName));
            }
            Schema finalSchema = new Schema(finalProjectedColumns);
            plan = new ProjectPlanNode(plan, finalSchema);
        }

        // 6. 排序层
        if (ast.orderByClause() != null) {
            plan = new SortPlanNode(plan, ast.orderByClause());
        }

        // 7. LIMIT 层
        if (ast.limitClause() != null) {
            plan = new LimitPlanNode(plan, ast.limitClause().limit());
        }

        return plan;
    }

    /**
     * 新增的辅助方法，用于递归地从表达式树中收集所有聚合函数节点。
     */
    private void collectAggregates(ExpressionNode node, List<AggregateExpressionNode> list) {
        if (node instanceof AggregateExpressionNode aggNode) {
            list.add(aggNode);
        } else if (node instanceof BinaryExpressionNode binNode) {
            collectAggregates(binNode.left(), list);
            collectAggregates(binNode.right(), list);
        }
    }

    private PlanNode createDeletePlan(DeleteStatementNode ast) {
        TableInfo tableInfo = catalog.getTable(ast.tableName().getName());
        PlanNode childPlan = new SeqScanPlanNode(tableInfo, ast.whereClause());
        if (ast.whereClause() != null) {
            childPlan = new FilterPlanNode(childPlan, ast.whereClause());
        }
        return new DeletePlanNode(childPlan, tableInfo);
    }

    private PlanNode createUpdatePlan(UpdateStatementNode ast) {
        TableInfo tableInfo = catalog.getTable(ast.tableName().getName());
        PlanNode childPlan = new SeqScanPlanNode(tableInfo, ast.whereClause());
        if (ast.whereClause() != null) {
            childPlan = new FilterPlanNode(childPlan, ast.whereClause());
        }
        return new UpdatePlanNode(childPlan, tableInfo, ast.setClauses());
    }

    private PlanNode createDropTablePlan(DropTableStatementNode ast) {
        return new DropTablePlanNode(ast.tableName().getName());
    }

    private PlanNode createAlterTablePlan(AlterTableStatementNode ast) {
        ColumnDefinitionNode colDef = ast.newColumnDefinition();
        Column newColumn =
            new Column(
                colDef.columnName().getName(),
                DataType.valueOf(colDef.dataType().getName().toUpperCase()));
        return new AlterTablePlanNode(ast.tableName().getName(), newColumn);
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
        if (predicate == null) {
            return null;
        }
        if (predicate instanceof BinaryExpressionNode binaryExpr) {
            if (binaryExpr.operator().type() == TokenType.EQUAL
                && binaryExpr.left() instanceof IdentifierNode
                && binaryExpr.right() instanceof LiteralNode) {

                String columnName = ((IdentifierNode) binaryExpr.left()).getName();
                return catalog.getIndex(tableName, columnName);
            }
        }
        return null;
    }

    private Value extractValueFromPredicate(ExpressionNode predicate) {
        if (predicate instanceof BinaryExpressionNode binaryExpr
            && binaryExpr.right() instanceof LiteralNode literal) {
            if (literal.literal().type() == TokenType.INTEGER_CONST) {
                return new Value(Integer.parseInt(literal.literal().lexeme()));
            } else if (literal.literal().type() == TokenType.STRING_CONST) {
                return new Value(literal.literal().lexeme());
            }
        }
        throw new IllegalStateException(
            "Could not extract value from predicate for index scan. This indicates a planner bug.");
    }

    @FunctionalInterface
    private interface StatementPlanner<T extends StatementNode> {
        PlanNode createPlan(T statement);
    }
}
