package com.indolyn.rill.core.infrastructure.transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;

class LogRecordBaselineTest {

    @Test
    void insertLogRecordShouldRoundTripCoreFields() {
        Tuple tuple = new Tuple(List.of(new Value(1), new Value("alice")));
        LogRecord original =
            new LogRecord(7, 11L, LogRecord.LogType.INSERT, "users", new RID(3, 5), tuple);
        original.setLsn(99L);

        LogRecord restored = LogRecord.fromBytes(ByteBuffer.wrap(original.toBytes()), null);

        assertEquals(99L, restored.getLsn());
        assertEquals(7, restored.getTransactionId());
        assertEquals(11L, restored.getPrevLSN());
        assertEquals(LogRecord.LogType.INSERT, restored.getLogType());
        assertEquals("users", restored.getTableName());
        assertEquals(3, restored.getRid().pageNum());
        assertEquals(5, restored.getRid().slotIndex());
        assertArrayEquals(tuple.toBytes(), restored.getTupleBytes());
    }

    @Test
    void createTableLogRecordShouldRoundTripSchema() {
        Schema schema =
            new Schema(
                List.of(
                    new Column("id", DataType.INT, "INT", List.of(), false, null, true),
                    new Column("name", DataType.VARCHAR, "VARCHAR", List.of(20), true, null, false)));
        LogRecord original =
            new LogRecord(1, -1L, LogRecord.LogType.CREATE_TABLE, "users", schema);
        original.setLsn(12L);

        LogRecord restored = LogRecord.fromBytes(ByteBuffer.wrap(original.toBytes()), null);

        assertEquals(LogRecord.LogType.CREATE_TABLE, restored.getLogType());
        assertEquals("users", restored.getTableName());
        assertEquals(List.of("id", "name"), restored.getSchema().getColumnNames());
        assertTrue(restored.getSchema().getColumn("id").isPrimaryKey());
    }
}
