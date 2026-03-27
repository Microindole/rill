package com.indolyn.rill.core.sql.type;

import com.indolyn.rill.core.sql.ast.type.TypeReferenceNode;

public interface SqlTypeResolver {

    ResolvedSqlType resolve(TypeReferenceNode typeReference);
}
