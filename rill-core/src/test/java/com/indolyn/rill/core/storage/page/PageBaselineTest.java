package com.indolyn.rill.core.storage.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;

import java.util.List;

import org.junit.jupiter.api.Test;

class PageBaselineTest {

    private static final Schema USER_SCHEMA =
        new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));

    @Test
    void pageShouldInsertReadMarkUndoAndDeleteTuples() {
        Page page = new Page(new PageId(7));
        Tuple alice = new Tuple(List.of(new Value(1), new Value("alice")));
        Tuple bob = new Tuple(List.of(new Value(2), new Value("bob")));

        assertTrue(page.insertTuple(alice));
        assertTrue(page.insertTuple(bob));
        assertEquals(2, page.getNumTuples());

        Tuple readBack = page.getTuple(0, USER_SCHEMA);
        assertNotNull(readBack);
        assertEquals(1, readBack.getValues().get(0).getValue());
        assertEquals("alice", readBack.getValues().get(1).getValue());

        assertTrue(page.markTupleAsDeleted(0));
        assertNull(page.getTuple(0, USER_SCHEMA));
        assertTrue(page.undoMarkTupleAsDeleted(0));
        assertNotNull(page.getTuple(0, USER_SCHEMA));

        assertTrue(page.deleteTuple(0));
        assertEquals(1, page.getNumTuples());
        Tuple remaining = page.getTuple(0, USER_SCHEMA);
        assertNotNull(remaining);
        assertEquals(2, remaining.getValues().get(0).getValue());
        assertEquals("bob", remaining.getValues().get(1).getValue());
    }

    @Test
    void pageShouldRejectTuplesWhenFreeSpaceIsExhausted() {
        Page page = new Page(new PageId(8));
        Tuple largeTuple = new Tuple(List.of(new Value(1), new Value("x".repeat(3000))));

        assertTrue(page.insertTuple(largeTuple));
        assertFalse(page.insertTuple(largeTuple));
    }
}
