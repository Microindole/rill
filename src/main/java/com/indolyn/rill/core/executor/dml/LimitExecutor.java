package com.indolyn.rill.core.executor.dml;

import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.common.model.Tuple;
import com.indolyn.rill.core.executor.TupleIterator;
import java.io.IOException;

/** Limit 执行器，限制从子执行器返回的元组数量。 */
public class LimitExecutor implements TupleIterator {

  private final TupleIterator child;
  private final int limit;
  private int count = 0;

  public LimitExecutor(TupleIterator child, int limit) {
    this.child = child;
    this.limit = limit;
  }

  @Override
  public Tuple next() throws IOException {
    if (hasNext()) {
      count++;
      return child.next();
    }
    return null;
  }

  @Override
  public boolean hasNext() throws IOException {
    return count < limit && child.hasNext();
  }

  @Override
  public Schema getOutputSchema() {
    return child.getOutputSchema();
  }
}
