package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.execution.predicate.AbstractPredicate;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.log.LogManager;

final class ExecutionSupport {
    private final BufferPoolManager bufferPoolManager;
    private final LogManager logManager;
    private final LockManager lockManager;
    private final PredicateFactory predicateFactory;

    ExecutionSupport(
        BufferPoolManager bufferPoolManager,
        LogManager logManager,
        LockManager lockManager,
        PredicateFactory predicateFactory) {
        this.bufferPoolManager = bufferPoolManager;
        this.logManager = logManager;
        this.lockManager = lockManager;
        this.predicateFactory = predicateFactory;
    }

    TableHeap createTableHeap(TableInfo tableInfo) {
        return new TableHeap(bufferPoolManager, tableInfo, logManager, lockManager);
    }

    AbstractPredicate createPredicate(ExpressionNode predicate, Schema schema) {
        if (predicate == null) {
            return null;
        }
        return predicateFactory.create(predicate, schema);
    }
}
