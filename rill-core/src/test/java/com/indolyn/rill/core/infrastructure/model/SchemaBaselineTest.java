package com.indolyn.rill.core.infrastructure.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class SchemaBaselineTest {

    @Test
    void schemaShouldNormalizePrimaryKeyAndPreserveColumnMetadata() throws Exception {
        Column id = new Column("id", DataType.INT);
        Column name = new Column("name", DataType.VARCHAR, "VARCHAR", List.of(20), true, null, false);

        Schema schema = new Schema(List.of(id, name), "id");

        assertEquals("id", schema.getPrimaryKeyColumnName());
        assertTrue(schema.getColumn("id").isPrimaryKey());
        assertFalse(schema.getColumn("id").isNullable());
        assertEquals(20, schema.getColumn("name").getLengthLimit());
    }

    @Test
    void schemaWriteReadShouldRoundTripTypeDeclarations() throws Exception {
        Schema original =
            new Schema(
                List.of(
                    new Column("id", DataType.INT, "INT", List.of(), false, null, true),
                    new Column("name", DataType.VARCHAR, "VARCHAR", List.of(32), true, "'guest'", false),
                    new Column("amount", DataType.DECIMAL, "NUMERIC", List.of(10, 2), true, null, false)));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(outputStream)) {
            original.write(out);
        }

        Schema restored;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            restored = Schema.read(in);
        }

        assertEquals(original.getColumnNames(), restored.getColumnNames());
        assertEquals("id", restored.getPrimaryKeyColumnName());
        assertEquals("VARCHAR(32)", restored.getColumn("name").formatTypeDeclaration());
        assertEquals("NUMERIC(10, 2)", restored.getColumn("amount").formatTypeDeclaration());
    }
}
