package com.indolyn.rill.core.execution.predicate;

import com.indolyn.rill.core.model.Tuple;

import java.io.IOException;

/**
 * 谓词的抽象基类，用于在 WHERE 子句中对元组进行求值。
 */
public abstract class AbstractPredicate {
    public abstract boolean evaluate(Tuple tuple) throws IOException;
}
