package com.indolyn.rill.core.sql.type;

import com.indolyn.rill.core.model.DataType;

public record ResolvedSqlType(String canonicalName, DataType physicalType) {
}
