package com.indolyn.rill.core.compiler.parser.ast.ddl;

import com.indolyn.rill.core.compiler.parser.ast.StatementNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;

public record CreateDatabaseStatementNode(IdentifierNode databaseName) implements StatementNode {}
