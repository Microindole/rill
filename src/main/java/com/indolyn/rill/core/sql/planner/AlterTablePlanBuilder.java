package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.AlterTablePlanNode;

class AlterTablePlanBuilder {

    PlanNode build(AlterTableStatementNode ast) {
        ColumnDefinitionNode columnDefinition = ast.newColumnDefinition();
        Column newColumn =
            new Column(
                columnDefinition.columnName().getName(),
                DataType.valueOf(columnDefinition.dataType().getName().toUpperCase()));
        return new AlterTablePlanNode(ast.tableName().getName(), newColumn);
    }
}
