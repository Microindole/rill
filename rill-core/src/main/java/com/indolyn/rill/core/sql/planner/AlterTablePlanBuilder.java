package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.AlterTablePlanNode;
import com.indolyn.rill.core.sql.type.SqlTypeResolver;

class AlterTablePlanBuilder {

    private final SqlTypeResolver sqlTypeResolver;

    AlterTablePlanBuilder(SqlTypeResolver sqlTypeResolver) {
        this.sqlTypeResolver = sqlTypeResolver;
    }

    PlanNode build(AlterTableStatementNode ast) {
        ColumnDefinitionNode columnDefinition = ast.newColumnDefinition();
        Column newColumn =
            new Column(
                columnDefinition.columnName().getName(),
                sqlTypeResolver.resolve(columnDefinition.dataType()).physicalType(),
                sqlTypeResolver.resolve(columnDefinition.dataType()).canonicalName(),
                columnDefinition.dataType().arguments(),
                columnDefinition.nullable(),
                renderDefaultLiteral(columnDefinition),
                columnDefinition.primaryKey());
        return new AlterTablePlanNode(ast.tableName().getName(), newColumn);
    }

    private String renderDefaultLiteral(ColumnDefinitionNode columnDefinition) {
        if (columnDefinition.defaultValue() == null) {
            return null;
        }
        return switch (columnDefinition.defaultValue().literal().type()) {
            case STRING_CONST -> "'" + columnDefinition.defaultValue().literal().lexeme() + "'";
            default -> columnDefinition.defaultValue().literal().lexeme().toUpperCase();
        };
    }
}
