package com.indolyn.rill.core.engine;

import com.indolyn.rill.core.DatabaseManager;
import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.common.model.Value;
import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.LiteralNode;
import com.indolyn.rill.core.compiler.planner.plan.*;
import com.indolyn.rill.core.compiler.planner.plan.dcl.CreateUserPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.dcl.GrantPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.ddl.*;
import com.indolyn.rill.core.compiler.planner.plan.dml.DeletePlanNode;
import com.indolyn.rill.core.compiler.planner.plan.dml.InsertPlanNode;
import com.indolyn.rill.core.compiler.planner.plan.dml.UpdatePlanNode;
import com.indolyn.rill.core.compiler.planner.plan.query.*;
import com.indolyn.rill.core.compiler.planner.plan.show.*;
import com.indolyn.rill.core.executor.*;
import com.indolyn.rill.core.executor.dcl.CreateUserExecutor;
import com.indolyn.rill.core.executor.dcl.GrantExecutor;
import com.indolyn.rill.core.executor.ddl.*;
import com.indolyn.rill.core.executor.dml.*;
import com.indolyn.rill.core.executor.expressions.AbstractPredicate;
import com.indolyn.rill.core.executor.expressions.ComparisonPredicate;
import com.indolyn.rill.core.executor.expressions.LogicalPredicate;
import com.indolyn.rill.core.executor.show.*;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionEngine {
    private final BufferPoolManager bufferPoolManager;
    private final Catalog catalog;
    private final LogManager logManager;
    private final LockManager lockManager;
    private final DatabaseManager dbManager;
    private final Map<Class<? extends PlanNode>, ExecutorFactory<? extends PlanNode>>
        executorFactories = new LinkedHashMap<>();

    public ExecutionEngine(
        BufferPoolManager bufferPoolManager,
        Catalog catalog,
        LogManager logManager,
        LockManager lockManager,
        DatabaseManager dbManager) { // <-- 新增
        this.bufferPoolManager = bufferPoolManager;
        this.catalog = catalog;
        this.logManager = logManager;
        this.lockManager = lockManager;
        this.dbManager = dbManager;
        registerExecutorFactories();
    }

    public TupleIterator execute(PlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        return buildExecutorTree(plan, txn);
    }

    private TupleIterator buildExecutorTree(PlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        ExecutorFactory<PlanNode> factory = resolveExecutorFactory(plan);
        if (factory != null) {
            return factory.create(plan, txn);
        }

        throw new UnsupportedOperationException(
            "Unsupported plan node: " + plan.getClass().getSimpleName());
    }

    private void registerExecutorFactories() {
        registerExecutorFactory(
            CreateDatabasePlanNode.class, (plan, txn) -> new CreateDatabaseExecutor(plan, dbManager));
        registerExecutorFactory(
            ShowDatabasesPlanNode.class, (plan, txn) -> new ShowDatabasesExecutor(plan, dbManager));
        registerExecutorFactory(
            ShowColumnsPlanNode.class, (plan, txn) -> new ShowColumnsExecutor(plan, catalog));
        registerExecutorFactory(
            ShowCreateTablePlanNode.class, (plan, txn) -> new ShowCreateTableExecutor(plan));
        registerExecutorFactory(
            DropDatabasePlanNode.class, (plan, txn) -> new DropDatabaseExecutor(plan, dbManager));
        registerExecutorFactory(InsertPlanNode.class, this::createInsertExecutor);
        registerExecutorFactory(SeqScanPlanNode.class, this::createSeqScanExecutor);
        registerExecutorFactory(DeletePlanNode.class, this::createDeleteExecutor);
        registerExecutorFactory(UpdatePlanNode.class, this::createUpdateExecutor);
        registerExecutorFactory(FilterPlanNode.class, this::createFilterExecutor);
        registerExecutorFactory(ProjectPlanNode.class, this::createProjectExecutor);
        registerExecutorFactory(
            SortPlanNode.class,
            (plan, txn) -> new SortExecutor(buildExecutorTree(plan.getChild(), txn), plan));
        registerExecutorFactory(
            LimitPlanNode.class,
            (plan, txn) -> new LimitExecutor(buildExecutorTree(plan.getChild(), txn), plan.getLimit()));
        registerExecutorFactory(JoinPlanNode.class, this::createJoinExecutor);
        registerExecutorFactory(CreateIndexPlanNode.class, this::createCreateIndexExecutor);
        registerExecutorFactory(IndexScanPlanNode.class, this::createIndexScanExecutor);
        registerExecutorFactory(
            ShowTablesPlanNode.class, (plan, txn) -> new ShowTablesExecutor(plan, catalog));
        registerExecutorFactory(CreateTablePlanNode.class, this::createTableExecutor);
        registerExecutorFactory(
            DropTablePlanNode.class,
            (plan, txn) -> new DropTableExecutor(plan, catalog, txn, logManager));
        registerExecutorFactory(
            AlterTablePlanNode.class,
            (plan, txn) -> new AlterTableExecutor(plan, catalog, txn, logManager));
        registerExecutorFactory(
            AggregatePlanNode.class,
            (plan, txn) -> new AggregateExecutor(buildExecutorTree(plan.getChild(), txn), plan));
        registerExecutorFactory(
            CreateUserPlanNode.class, (plan, txn) -> new CreateUserExecutor(plan, catalog, txn));
        registerExecutorFactory(
            GrantPlanNode.class, (plan, txn) -> new GrantExecutor(plan, catalog, txn));
    }

    private InsertExecutor createInsertExecutor(InsertPlanNode plan, Transaction txn) {
        TableHeap tableHeap =
            new TableHeap(bufferPoolManager, plan.getTableInfo(), logManager, lockManager);
        return new InsertExecutor(plan, tableHeap, txn, catalog, bufferPoolManager);
    }

    private SeqScanExecutor createSeqScanExecutor(SeqScanPlanNode plan, Transaction txn)
        throws IOException {
        TableHeap tableHeap =
            new TableHeap(bufferPoolManager, plan.getTableInfo(), logManager, lockManager);
        AbstractPredicate predicate = null;
        if (plan.getPredicate() != null) {
            predicate = createPredicateFromAst(plan.getPredicate(), plan.getOutputSchema());
        }
        return new SeqScanExecutor(tableHeap, txn, predicate);
    }

    private DeleteExecutor createDeleteExecutor(DeletePlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childPlan = buildExecutorTree(plan.getChild(), txn);
        TableHeap tableHeap =
            new TableHeap(bufferPoolManager, plan.getTableInfo(), logManager, lockManager);
        return new DeleteExecutor(plan, childPlan, tableHeap, txn, catalog, bufferPoolManager);
    }

    private UpdateExecutor createUpdateExecutor(UpdatePlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childPlan = buildExecutorTree(plan.getChild(), txn);
        TableHeap tableHeap =
            new TableHeap(bufferPoolManager, plan.getTableInfo(), logManager, lockManager);
        return new UpdateExecutor(
            childPlan,
            tableHeap,
            plan.getTableInfo().getSchema(),
            plan.getSetClauses(),
            txn,
            catalog,
            bufferPoolManager);
    }

    private FilterExecutor createFilterExecutor(FilterPlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childExecutor = buildExecutorTree(plan.getChild(), txn);
        AbstractPredicate predicate =
            createPredicateFromAst(plan.getPredicate(), plan.getChild().getOutputSchema());
        return new FilterExecutor(childExecutor, predicate);
    }

    private ProjectExecutor createProjectExecutor(ProjectPlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childExecutor = buildExecutorTree(plan.getChild(), txn);
        List<Integer> columnIndexes = new ArrayList<>();
        Schema childSchema = plan.getChild().getOutputSchema();
        for (String colName :
            plan.getOutputSchema().getColumns().stream()
                .map(c -> c.getName())
                .collect(Collectors.toList())) {
            for (int i = 0; i < childSchema.getColumns().size(); i++) {
                if (childSchema.getColumns().get(i).getName().equalsIgnoreCase(colName)) {
                    columnIndexes.add(i);
                    break;
                }
            }
        }
        return new ProjectExecutor(childExecutor, columnIndexes);
    }

    private JoinExecutor createJoinExecutor(JoinPlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator leftExecutor = buildExecutorTree(plan.getLeft(), txn);
        TupleIterator rightExecutor = buildExecutorTree(plan.getRight(), txn);
        return new JoinExecutor(plan, leftExecutor, rightExecutor);
    }

    private CreateIndexExecutor createCreateIndexExecutor(CreateIndexPlanNode plan, Transaction txn) {
        TableHeap tableHeap =
            new TableHeap(bufferPoolManager, plan.getTableInfo(), logManager, lockManager);
        return new CreateIndexExecutor(plan, tableHeap, catalog, bufferPoolManager, txn);
    }

    private IndexScanExecutor createIndexScanExecutor(IndexScanPlanNode plan, Transaction txn) {
        TableHeap tableHeap =
            new TableHeap(bufferPoolManager, plan.getTableInfo(), logManager, lockManager);
        return new IndexScanExecutor(plan, tableHeap, bufferPoolManager, txn);
    }

    private CreateTableExecutor createTableExecutor(CreateTablePlanNode plan, Transaction txn) {
        return new CreateTableExecutor(plan, catalog, txn, logManager, bufferPoolManager, lockManager);
    }

    private <T extends PlanNode> void registerExecutorFactory(
        Class<T> planType, ExecutorFactory<T> factory) {
        executorFactories.put(planType, factory);
    }

    @SuppressWarnings("unchecked")
    private ExecutorFactory<PlanNode> resolveExecutorFactory(PlanNode plan) {
        ExecutorFactory<? extends PlanNode> exactFactory = executorFactories.get(plan.getClass());
        if (exactFactory != null) {
            return (ExecutorFactory<PlanNode>) exactFactory;
        }

        for (Map.Entry<Class<? extends PlanNode>, ExecutorFactory<? extends PlanNode>> entry :
            executorFactories.entrySet()) {
            if (entry.getKey().isInstance(plan)) {
                return (ExecutorFactory<PlanNode>) entry.getValue();
            }
        }
        return null;
    }

    private AbstractPredicate createPredicateFromAst(ExpressionNode expression, Schema schema) {
        if (expression instanceof BinaryExpressionNode node) {
            String operatorName = node.operator().type().name();
            // 检查是否是逻辑运算符
            if ("AND".equals(operatorName) || "OR".equals(operatorName)) {
                AbstractPredicate left = createPredicateFromAst(node.left(), schema);
                AbstractPredicate right = createPredicateFromAst(node.right(), schema);
                return new LogicalPredicate(left, right, operatorName);
            }

            // 否则，是比较运算符
            if (!(node.left() instanceof IdentifierNode) || !(node.right() instanceof LiteralNode)) {
                throw new UnsupportedOperationException(
                    "WHERE clause only supports 'column_name op literal' format.");
            }

            String columnName = ((IdentifierNode) node.left()).getName();
            int columnIndex = getColumnIndex(schema, columnName);
            Value literalValue = getLiteralValue((LiteralNode) node.right());

            return new ComparisonPredicate(columnIndex, literalValue, operatorName);
        }
        throw new UnsupportedOperationException(
            "Unsupported expression type in WHERE clause: " + expression.getClass().getSimpleName());
    }

    private int getColumnIndex(Schema schema, String columnName) {
        for (int i = 0; i < schema.getColumns().size(); i++) {
            if (schema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new IllegalStateException(
            "Column '" + columnName + "' not found in schema during execution planning.");
    }

    private Value getLiteralValue(LiteralNode literalNode) {
        String lexeme = literalNode.literal().lexeme();
        return switch (literalNode.literal().type()) {
            case INTEGER_CONST -> new Value(Integer.parseInt(lexeme));
            case STRING_CONST -> new Value(lexeme);
            default -> throw new IllegalStateException("Unsupported literal type in expression.");
        };
    }

    @FunctionalInterface
    private interface ExecutorFactory<T extends PlanNode> {
        TupleIterator create(T plan, Transaction txn) throws IOException, InterruptedException;
    }
}
