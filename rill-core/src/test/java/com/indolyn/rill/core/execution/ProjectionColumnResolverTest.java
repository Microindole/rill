package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProjectionColumnResolverTest {

    @Test
    void resolverShouldMapOutputColumnsBackToChildSchemaIndexes() {
        ProjectionColumnResolver resolver = new ProjectionColumnResolver();
        Schema childSchema =
            new Schema(
                List.of(
                    new Column("id", DataType.INT),
                    new Column("name", DataType.VARCHAR),
                    new Column("email", DataType.VARCHAR)));
        Schema outputSchema =
            new Schema(List.of(new Column("email", DataType.VARCHAR), new Column("id", DataType.INT)));

        assertEquals(List.of(2, 0), resolver.resolve(childSchema, outputSchema));
    }

    @Test
    void resolverShouldMatchColumnNamesCaseInsensitively() {
        ProjectionColumnResolver resolver = new ProjectionColumnResolver();
        Schema childSchema =
            new Schema(List.of(new Column("UserId", DataType.INT), new Column("UserName", DataType.VARCHAR)));
        Schema outputSchema =
            new Schema(List.of(new Column("username", DataType.VARCHAR), new Column("userid", DataType.INT)));

        assertEquals(List.of(1, 0), resolver.resolve(childSchema, outputSchema));
    }
}
