package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateTablePlanNode;

import java.util.List;
import java.util.stream.Collectors;

class CreateTablePlanBuilder {

    PlanNode build(CreateTableStatementNode ast) {
        String tableName = ast.tableName().getName();
        List<Column> columns =
            ast.columns().stream()
                .map(
                    colDef ->
                        new Column(
                            colDef.columnName().getName(),
                            DataType.valueOf(colDef.dataType().getName().toUpperCase())))
                .collect(Collectors.toList());
        Schema schema =
            new Schema(
                columns, ast.primaryKeyColumn() != null ? ast.primaryKeyColumn().getName() : null);
        return new CreateTablePlanNode(tableName, schema);
    }
}
