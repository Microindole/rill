package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateTablePlanNode;
import com.indolyn.rill.core.sql.type.SqlTypeResolver;

import java.util.List;
import java.util.stream.Collectors;

class CreateTablePlanBuilder {

    private final SqlTypeResolver sqlTypeResolver;

    CreateTablePlanBuilder(SqlTypeResolver sqlTypeResolver) {
        this.sqlTypeResolver = sqlTypeResolver;
    }

    PlanNode build(CreateTableStatementNode ast) {
        String tableName = ast.tableName().getName();
        List<Column> columns =
            ast.columns().stream()
                .map(
                    colDef ->
                        new Column(
                            colDef.columnName().getName(),
                            sqlTypeResolver.resolve(colDef.dataType()).physicalType(),
                            sqlTypeResolver.resolve(colDef.dataType()).canonicalName(),
                            colDef.dataType().arguments(),
                            colDef.nullable(),
                            renderDefaultLiteral(colDef),
                            colDef.primaryKey()))
                .collect(Collectors.toList());
        Schema schema = new Schema(columns);
        return new CreateTablePlanNode(tableName, schema);
    }

    private String renderDefaultLiteral(com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode columnDefinition) {
        if (columnDefinition.defaultValue() == null) {
            return null;
        }
        return switch (columnDefinition.defaultValue().literal().type()) {
            case STRING_CONST -> "'" + columnDefinition.defaultValue().literal().lexeme() + "'";
            default -> columnDefinition.defaultValue().literal().lexeme().toUpperCase();
        };
    }
}
