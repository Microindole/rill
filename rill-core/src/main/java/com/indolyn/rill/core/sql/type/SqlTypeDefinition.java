package com.indolyn.rill.core.sql.type;

import com.indolyn.rill.core.model.DataType;

import java.util.List;

record SqlTypeDefinition(
    String canonicalName,
    DataType physicalType,
    int minArguments,
    int maxArguments,
    List<String> aliases) {

    boolean supportsArgumentCount(int count) {
        return count >= minArguments && count <= maxArguments;
    }

    ResolvedSqlType toResolvedType() {
        return new ResolvedSqlType(canonicalName, physicalType);
    }
}
