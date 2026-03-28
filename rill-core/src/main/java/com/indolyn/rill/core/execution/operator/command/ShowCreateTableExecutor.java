package com.indolyn.rill.core.execution.operator.command;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.command.ShowCreateTablePlanNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ShowCreateTableExecutor implements TupleIterator {

    private final ShowCreateTablePlanNode plan;
    private final Catalog catalog;
    private Iterator<Tuple> resultIterator;

    public ShowCreateTableExecutor(ShowCreateTablePlanNode plan, Catalog catalog) {
        this.plan = plan;
        this.catalog = catalog;
    }

    @Override
    public Tuple next() throws IOException {
        if (resultIterator == null) {
            generateResult();
        }
        return resultIterator.hasNext() ? resultIterator.next() : null;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (resultIterator == null) {
            generateResult();
        }
        return resultIterator.hasNext();
    }

    @Override
    public Schema getOutputSchema() {
        return plan.getOutputSchema();
    }

    private void generateResult() {
        TableInfo tableInfo = catalog.getTable(plan.getTableName());
        if (tableInfo == null) {
            throw new IllegalStateException(
                "Table '" + plan.getTableName() + "' not found during execution.");
        }

        String ddl =
            buildShowCreateTableSql(
                plan.getTableName(), tableInfo.getSchema(), catalog.getIndexesForTable(plan.getTableName()));

        List<Tuple> tuples = new ArrayList<>();
        tuples.add(new Tuple(List.of(new Value(plan.getTableName()), new Value(ddl))));
        resultIterator = tuples.iterator();
    }

    private String buildShowCreateTableSql(String tableName, Schema schema, List<IndexInfo> indexes) {
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE `").append(tableName).append("` (\n");

        List<String> definitions = new ArrayList<>();
        for (Column col : schema.getColumns()) {
            definitions.add(buildColumnDefinitionSql(col));
        }

        String primaryKeyColumnName = resolvePrimaryKeyColumnName(schema, indexes);
        if (primaryKeyColumnName != null) {
            definitions.add("  PRIMARY KEY (`" + primaryKeyColumnName + "`)");
        }

        for (IndexInfo indexInfo : indexes) {
            if (primaryKeyColumnName != null
                && indexInfo.getColumnName().equalsIgnoreCase(primaryKeyColumnName)) {
                continue;
            }
            definitions.add(
                "  KEY `"
                    + indexInfo.getIndexName()
                    + "` (`"
                    + indexInfo.getColumnName()
                    + "`)");
        }

        createTableSql.append(String.join(",\n", definitions));
        createTableSql.append("\n)");
        return createTableSql.toString();
    }

    private String buildColumnDefinitionSql(Column column) {
        StringBuilder definition =
            new StringBuilder("  `")
                .append(column.getName())
                .append("` ")
                .append(column.formatTypeDeclaration());
        if (!column.isNullable()) {
            definition.append(" NOT NULL");
        }
        if (column.hasDefaultValue()) {
            definition.append(" DEFAULT ").append(column.getDefaultValue());
        }
        return definition.toString();
    }

    private String resolvePrimaryKeyColumnName(Schema schema, List<IndexInfo> indexes) {
        if (schema.getPrimaryKeyColumnName() != null) {
            return schema.getPrimaryKeyColumnName();
        }
        for (Column column : schema.getColumns()) {
            if (column.isPrimaryKey()) {
                return column.getName();
            }
        }
        for (IndexInfo indexInfo : indexes) {
            if (indexInfo.getIndexName().toLowerCase(Locale.ROOT).startsWith("pk_")) {
                return indexInfo.getColumnName();
            }
        }
        return null;
    }
}
