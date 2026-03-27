package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.execution.predicate.AbstractPredicate;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.log.LogService;

final class ExecutionSupport {
    private final PageAccess pageAccess;
    private final LogService logManager;
    private final LockService lockManager;
    private final PredicateFactory predicateFactory;

    ExecutionSupport(
        PageAccess pageAccess,
        LogService logManager,
        LockService lockManager,
        PredicateFactory predicateFactory) {
        this.pageAccess = pageAccess;
        this.logManager = logManager;
        this.lockManager = lockManager;
        this.predicateFactory = predicateFactory;
    }

    TableHeap createTableHeap(TableInfo tableInfo) {
        return new TableHeap(pageAccess, tableInfo, logManager, lockManager);
    }

    AbstractPredicate createPredicate(ExpressionNode predicate, Schema schema) {
        if (predicate == null) {
            return null;
        }
        return predicateFactory.create(predicate, schema);
    }
}
