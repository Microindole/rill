package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.execution.operator.command.CreateIndexExecutor;
import com.indolyn.rill.core.execution.operator.command.CreateTableExecutor;
import com.indolyn.rill.core.execution.operator.query.DeleteExecutor;
import com.indolyn.rill.core.execution.operator.query.FilterExecutor;
import com.indolyn.rill.core.execution.operator.query.IndexScanExecutor;
import com.indolyn.rill.core.execution.operator.query.InsertExecutor;
import com.indolyn.rill.core.execution.operator.query.JoinExecutor;
import com.indolyn.rill.core.execution.operator.query.ProjectExecutor;
import com.indolyn.rill.core.execution.operator.query.SeqScanExecutor;
import com.indolyn.rill.core.execution.operator.query.UpdateExecutor;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateIndexPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DeletePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.UpdatePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.FilterPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.IndexScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.JoinPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.ProjectPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogService;

import java.io.IOException;

class QueryExecutorBuilder {

    @FunctionalInterface
    interface ChildExecutorBuilder {
        TupleIterator build(PlanNode plan, Transaction txn) throws IOException, InterruptedException;
    }

    private final Catalog catalog;
    private final BufferPoolManager bufferPoolManager;
    private final LogService logManager;
    private final LockService lockManager;
    private final ExecutionSupport executionSupport;
    private final ProjectionColumnResolver projectionColumnResolver;
    private final ChildExecutorBuilder childExecutorBuilder;

    QueryExecutorBuilder(
        Catalog catalog,
        BufferPoolManager bufferPoolManager,
        LogService logManager,
        LockService lockManager,
        ExecutionSupport executionSupport,
        ProjectionColumnResolver projectionColumnResolver,
        ChildExecutorBuilder childExecutorBuilder) {
        this.catalog = catalog;
        this.bufferPoolManager = bufferPoolManager;
        this.logManager = logManager;
        this.lockManager = lockManager;
        this.executionSupport = executionSupport;
        this.projectionColumnResolver = projectionColumnResolver;
        this.childExecutorBuilder = childExecutorBuilder;
    }

    InsertExecutor createInsertExecutor(InsertPlanNode plan, Transaction txn) {
        TableHeap tableHeap = executionSupport.createTableHeap(plan.getTableInfo());
        return new InsertExecutor(plan, tableHeap, txn, catalog, bufferPoolManager);
    }

    SeqScanExecutor createSeqScanExecutor(SeqScanPlanNode plan, Transaction txn) throws IOException {
        TableHeap tableHeap = executionSupport.createTableHeap(plan.getTableInfo());
        var predicate = executionSupport.createPredicate(plan.getPredicate(), plan.getOutputSchema());
        return new SeqScanExecutor(tableHeap, txn, predicate);
    }

    DeleteExecutor createDeleteExecutor(DeletePlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childPlan = childExecutorBuilder.build(plan.getChild(), txn);
        TableHeap tableHeap = executionSupport.createTableHeap(plan.getTableInfo());
        return new DeleteExecutor(plan, childPlan, tableHeap, txn, catalog, bufferPoolManager);
    }

    UpdateExecutor createUpdateExecutor(UpdatePlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childPlan = childExecutorBuilder.build(plan.getChild(), txn);
        TableHeap tableHeap = executionSupport.createTableHeap(plan.getTableInfo());
        return new UpdateExecutor(
            childPlan,
            tableHeap,
            plan.getTableInfo().getSchema(),
            plan.getSetClauses(),
            txn,
            catalog,
            bufferPoolManager);
    }

    FilterExecutor createFilterExecutor(FilterPlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childExecutor = childExecutorBuilder.build(plan.getChild(), txn);
        var predicate =
            executionSupport.createPredicate(plan.getPredicate(), plan.getChild().getOutputSchema());
        return new FilterExecutor(childExecutor, predicate);
    }

    ProjectExecutor createProjectExecutor(ProjectPlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator childExecutor = childExecutorBuilder.build(plan.getChild(), txn);
        var columnIndexes =
            projectionColumnResolver.resolve(plan.getChild().getOutputSchema(), plan.getOutputSchema());
        return new ProjectExecutor(childExecutor, columnIndexes);
    }

    JoinExecutor createJoinExecutor(JoinPlanNode plan, Transaction txn)
        throws IOException, InterruptedException {
        TupleIterator leftExecutor = childExecutorBuilder.build(plan.getLeft(), txn);
        TupleIterator rightExecutor = childExecutorBuilder.build(plan.getRight(), txn);
        return new JoinExecutor(plan, leftExecutor, rightExecutor);
    }

    CreateIndexExecutor createCreateIndexExecutor(CreateIndexPlanNode plan, Transaction txn) {
        TableHeap tableHeap = executionSupport.createTableHeap(plan.getTableInfo());
        return new CreateIndexExecutor(plan, tableHeap, catalog, bufferPoolManager, txn);
    }

    IndexScanExecutor createIndexScanExecutor(IndexScanPlanNode plan, Transaction txn) {
        TableHeap tableHeap = executionSupport.createTableHeap(plan.getTableInfo());
        return new IndexScanExecutor(plan, tableHeap, bufferPoolManager, txn);
    }

    CreateTableExecutor createTableExecutor(CreateTablePlanNode plan, Transaction txn) {
        return new CreateTableExecutor(plan, catalog, txn, logManager, bufferPoolManager, lockManager);
    }
}
