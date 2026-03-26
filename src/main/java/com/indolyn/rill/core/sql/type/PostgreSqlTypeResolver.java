package com.indolyn.rill.core.sql.type;

import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.ast.type.TypeReferenceNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostgreSqlTypeResolver implements SqlTypeResolver {

    private final Map<String, SqlTypeDefinition> definitions = new LinkedHashMap<>();

    public PostgreSqlTypeResolver() {
        register("SMALLINT", DataType.SMALLINT, 0, 0, List.of("INT2"));
        register("INT", DataType.INT, 0, 0, List.of("INTEGER"));
        register("BIGINT", DataType.BIGINT, 0, 0, List.of("INT8"));
        register("VARCHAR", DataType.VARCHAR, 0, 1, List.of("CHARACTER VARYING", "TEXT"));
        register("CHAR", DataType.CHAR, 0, 1, List.of("CHARACTER"));
        register("DECIMAL", DataType.DECIMAL, 0, 2, List.of("NUMERIC"));
        register("DATE", DataType.DATE, 0, 0, List.of());
        register("TIMESTAMP", DataType.TIMESTAMP, 0, 0, List.of("TIMESTAMP WITHOUT TIME ZONE"));
        register("BOOLEAN", DataType.BOOLEAN, 0, 0, List.of("BOOL"));
        register("FLOAT", DataType.FLOAT, 0, 0, List.of("REAL", "FLOAT4"));
        register("DOUBLE", DataType.DOUBLE, 0, 0, List.of("DOUBLE PRECISION", "FLOAT8"));
    }

    @Override
    public ResolvedSqlType resolve(TypeReferenceNode typeReference) {
        String normalizedName = normalize(typeReference.normalizedName());
        if ("TEXT".equals(normalizedName) && !typeReference.arguments().isEmpty()) {
            throw new SemanticException("Data type '" + typeReference.displayName() + "' does not support arguments.");
        }
        SqlTypeDefinition definition = definitions.get(normalizedName);
        if (definition == null) {
            throw new SemanticException("Unsupported PostgreSQL data type '" + typeReference.displayName() + "'.");
        }
        int argumentCount = typeReference.arguments().size();
        if (!definition.supportsArgumentCount(argumentCount)) {
            throw new SemanticException(
                "Data type '"
                    + typeReference.displayName()
                    + "' does not support "
                    + argumentCount
                    + " argument(s).");
        }
        validateArguments(typeReference, definition);
        return definition.toResolvedType();
    }

    private void validateArguments(TypeReferenceNode typeReference, SqlTypeDefinition definition) {
        List<Integer> arguments = typeReference.arguments();
        if (arguments.isEmpty()) {
            return;
        }

        for (Integer argument : arguments) {
            if (argument == null || argument <= 0) {
                throw new SemanticException(
                    "Data type '" + typeReference.displayName() + "' requires positive argument values.");
            }
        }

        if ("DECIMAL".equals(definition.canonicalName()) && arguments.size() == 2) {
            int precision = arguments.get(0);
            int scale = arguments.get(1);
            if (scale > precision) {
                throw new SemanticException(
                    "Data type '"
                        + typeReference.displayName()
                        + "' requires scale to be less than or equal to precision.");
            }
        }
    }

    private void register(
        String canonicalName,
        DataType physicalType,
        int minArguments,
        int maxArguments,
        List<String> aliases) {
        SqlTypeDefinition definition =
            new SqlTypeDefinition(canonicalName, physicalType, minArguments, maxArguments, aliases);
        definitions.put(normalize(canonicalName), definition);
        for (String alias : aliases) {
            definitions.put(normalize(alias), definition);
        }
    }

    private String normalize(String typeName) {
        return typeName.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
