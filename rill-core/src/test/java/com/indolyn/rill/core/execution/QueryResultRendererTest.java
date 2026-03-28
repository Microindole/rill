package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class QueryResultRendererTest {

    @Test
    void renderShouldReturnQueryOkForNullIterator() throws Exception {
        QueryResultRenderer renderer = new QueryResultRenderer();

        assertEquals("Query OK.", renderer.render((TupleIterator) null));
    }

    @Test
    void renderShouldFormatSelectResultsAsAsciiTable() {
        QueryResultRenderer renderer = new QueryResultRenderer();
        QueryResult queryResult =
            QueryResult.newSelectResult(
                new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))),
                List.of(new Tuple(List.of(new Value(1), new Value("alice")))));

        String rendered = renderer.render(queryResult);

        assertTrue(rendered.contains("| id | name  |"));
        assertTrue(rendered.contains("| 1  | alice |"));
        assertTrue(rendered.contains("Query finished, 1 rows returned."));
    }

    @Test
    void renderShouldFormatAffectedRowsFromIteratorSchema() throws Exception {
        QueryResultRenderer renderer = new QueryResultRenderer();
        TupleIterator iterator =
            new SimpleIterator(
                new Schema(List.of(new Column("updated_rows", DataType.INT))),
                List.of(new Tuple(List.of(new Value(3)))));

        assertEquals("Query OK, 3 rows affected.", renderer.render(iterator));
    }

    private static final class SimpleIterator implements TupleIterator {
        private final Schema outputSchema;
        private final Iterator<Tuple> iterator;

        private SimpleIterator(Schema outputSchema, List<Tuple> tuples) {
            this.outputSchema = outputSchema;
            this.iterator = tuples.iterator();
        }

        @Override
        public Tuple next() throws IOException {
            return iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public boolean hasNext() throws IOException {
            return iterator.hasNext();
        }

        @Override
        public Schema getOutputSchema() {
            return outputSchema;
        }
    }
}
