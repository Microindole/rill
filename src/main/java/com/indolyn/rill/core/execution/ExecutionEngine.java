package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.execution.operator.command.AlterTableExecutor;
import com.indolyn.rill.core.execution.operator.command.CreateDatabaseExecutor;
import com.indolyn.rill.core.execution.operator.command.CreateUserExecutor;
import com.indolyn.rill.core.execution.operator.command.DropDatabaseExecutor;
import com.indolyn.rill.core.execution.operator.command.DropTableExecutor;
import com.indolyn.rill.core.execution.operator.command.GrantExecutor;
import com.indolyn.rill.core.execution.operator.command.ShowColumnsExecutor;
import com.indolyn.rill.core.execution.operator.command.ShowCreateTableExecutor;
import com.indolyn.rill.core.execution.operator.command.ShowDatabasesExecutor;
import com.indolyn.rill.core.execution.operator.command.ShowTablesExecutor;
import com.indolyn.rill.core.execution.operator.query.AggregateExecutor;
import com.indolyn.rill.core.execution.operator.query.LimitExecutor;
import com.indolyn.rill.core.execution.operator.query.SortExecutor;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.AlterTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateIndexPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateUserPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DeletePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DropDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DropTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.GrantPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowColumnsPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowCreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowDatabasesPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowTablesPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.UpdatePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.AggregatePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.FilterPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.IndexScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.JoinPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.LimitPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.ProjectPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SortPlanNode;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionEngine {
    private final Catalog catalog;
    private final LogManager logManager;
    private final DatabaseManager dbManager;
    private final QueryExecutorBuilder queryExecutorBuilder;
    private final Map<Class<? extends PlanNode>, ExecutorFactory<? extends PlanNode>>
        executorFactories = new LinkedHashMap<>();

    public ExecutionEngine(
        BufferPoolManager bufferPoolManager,
        Catalog catalog,
        LogManager logManager,
        LockManager lockManager,
        DatabaseManager dbManager) {
        this.catalog = catalog;
        this.logManager = logManager;
        this.dbManager = dbManager;
        ExecutionSupport executionSupport =
            new ExecutionSupport(bufferPoolManager, logManager, lockManager, new PredicateFactory());
        ProjectionColumnResolver projectionColumnResolver = new ProjectionColumnResolver();
        this.queryExecutorBuilder =
            new QueryExecutorBuilder(
                catalog,
                bufferPoolManager,
                logManager,
                lockManager,
                executionSupport,
                projectionColumnResolver,
                this::buildExecutorTree);
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
        registerCommandExecutorFactories();
        registerQueryExecutorFactories();
    }

    private void registerCommandExecutorFactories() {
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
        registerExecutorFactory(
            ShowTablesPlanNode.class, (plan, txn) -> new ShowTablesExecutor(plan, catalog));
        registerExecutorFactory(
            DropTablePlanNode.class,
            (plan, txn) -> new DropTableExecutor(plan, catalog, txn, logManager));
        registerExecutorFactory(
            AlterTablePlanNode.class,
            (plan, txn) -> new AlterTableExecutor(plan, catalog, txn, logManager));
        registerExecutorFactory(
            CreateUserPlanNode.class, (plan, txn) -> new CreateUserExecutor(plan, catalog, txn));
        registerExecutorFactory(
            GrantPlanNode.class, (plan, txn) -> new GrantExecutor(plan, catalog, txn));
    }

    private void registerQueryExecutorFactories() {
        registerExecutorFactory(InsertPlanNode.class, queryExecutorBuilder::createInsertExecutor);
        registerExecutorFactory(SeqScanPlanNode.class, queryExecutorBuilder::createSeqScanExecutor);
        registerExecutorFactory(DeletePlanNode.class, queryExecutorBuilder::createDeleteExecutor);
        registerExecutorFactory(UpdatePlanNode.class, queryExecutorBuilder::createUpdateExecutor);
        registerExecutorFactory(FilterPlanNode.class, queryExecutorBuilder::createFilterExecutor);
        registerExecutorFactory(ProjectPlanNode.class, queryExecutorBuilder::createProjectExecutor);
        registerExecutorFactory(
            SortPlanNode.class,
            (plan, txn) -> new SortExecutor(buildExecutorTree(plan.getChild(), txn), plan));
        registerExecutorFactory(
            LimitPlanNode.class,
            (plan, txn) -> new LimitExecutor(buildExecutorTree(plan.getChild(), txn), plan.getLimit()));
        registerExecutorFactory(JoinPlanNode.class, queryExecutorBuilder::createJoinExecutor);
        registerExecutorFactory(
            CreateIndexPlanNode.class, queryExecutorBuilder::createCreateIndexExecutor);
        registerExecutorFactory(IndexScanPlanNode.class, queryExecutorBuilder::createIndexScanExecutor);
        registerExecutorFactory(CreateTablePlanNode.class, queryExecutorBuilder::createTableExecutor);
        registerExecutorFactory(
            AggregatePlanNode.class,
            (plan, txn) -> new AggregateExecutor(buildExecutorTree(plan.getChild(), txn), plan));
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

    @FunctionalInterface
    private interface ExecutorFactory<T extends PlanNode> {
        TupleIterator create(T plan, Transaction txn) throws IOException, InterruptedException;
    }
}
