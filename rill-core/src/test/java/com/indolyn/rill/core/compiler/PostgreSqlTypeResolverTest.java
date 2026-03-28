package com.indolyn.rill.core.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.ast.type.TypeReferenceNode;
import com.indolyn.rill.core.sql.type.PostgreSqlTypeResolver;
import com.indolyn.rill.core.sql.type.ResolvedSqlType;

import java.util.List;

import org.junit.jupiter.api.Test;

class PostgreSqlTypeResolverTest {

    @Test
    void resolverShouldNormalizeAliasesToCanonicalTypes() {
        PostgreSqlTypeResolver resolver = new PostgreSqlTypeResolver();

        ResolvedSqlType integerType = resolver.resolve(new TypeReferenceNode(List.of("integer"), List.of()));
        ResolvedSqlType timestampType =
            resolver.resolve(new TypeReferenceNode(List.of("timestamp", "without", "time", "zone"), List.of()));
        ResolvedSqlType doubleType =
            resolver.resolve(new TypeReferenceNode(List.of("double", "precision"), List.of()));

        assertEquals("INT", integerType.canonicalName());
        assertEquals(DataType.INT, integerType.physicalType());
        assertEquals("TIMESTAMP", timestampType.canonicalName());
        assertEquals(DataType.TIMESTAMP, timestampType.physicalType());
        assertEquals("DOUBLE", doubleType.canonicalName());
        assertEquals(DataType.DOUBLE, doubleType.physicalType());
    }

    @Test
    void resolverShouldAcceptSupportedTypeArguments() {
        PostgreSqlTypeResolver resolver = new PostgreSqlTypeResolver();

        ResolvedSqlType varcharType = resolver.resolve(new TypeReferenceNode(List.of("varchar"), List.of(32)));
        ResolvedSqlType decimalType =
            resolver.resolve(new TypeReferenceNode(List.of("numeric"), List.of(10, 2)));

        assertEquals("VARCHAR", varcharType.canonicalName());
        assertEquals(DataType.VARCHAR, varcharType.physicalType());
        assertEquals("DECIMAL", decimalType.canonicalName());
        assertEquals(DataType.DECIMAL, decimalType.physicalType());
    }

    @Test
    void resolverShouldRejectUnsupportedOrInvalidTypeDeclarations() {
        PostgreSqlTypeResolver resolver = new PostgreSqlTypeResolver();

        SemanticException textWithArgs =
            assertThrows(
                SemanticException.class,
                () -> resolver.resolve(new TypeReferenceNode(List.of("text"), List.of(20))));
        assertTrue(textWithArgs.getMessage().contains("does not support arguments"));

        SemanticException badScale =
            assertThrows(
                SemanticException.class,
                () -> resolver.resolve(new TypeReferenceNode(List.of("numeric"), List.of(4, 8))));
        assertTrue(badScale.getMessage().contains("scale"));

        SemanticException badArgument =
            assertThrows(
                SemanticException.class,
                () -> resolver.resolve(new TypeReferenceNode(List.of("varchar"), List.of(0))));
        assertTrue(badArgument.getMessage().contains("positive"));

        SemanticException unknownType =
            assertThrows(
                SemanticException.class,
                () -> resolver.resolve(new TypeReferenceNode(List.of("jsonb"), List.of())));
        assertTrue(unknownType.getMessage().contains("Unsupported PostgreSQL data type"));
    }
}
