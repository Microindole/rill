package com.indolyn.rill.core.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

/**
 * 定义表的模式，包含列的定义。
 */
@Getter
public class Schema {
    private final List<Column> columns;
    private String primaryKeyColumnName;

    public Schema(List<Column> columns) {
        this.columns = List.copyOf(columns);
        this.primaryKeyColumnName =
            this.columns.stream()
                .filter(Column::isPrimaryKey)
                .map(Column::getName)
                .findFirst()
                .orElse(null);
    }

    public Schema(List<Column> columns, String primaryKeyColumnName) {
        this(normalizePrimaryKeyColumns(columns, primaryKeyColumnName));
    }

    private static List<Column> normalizePrimaryKeyColumns(
        List<Column> columns, String primaryKeyColumnName) {
        if (primaryKeyColumnName == null || primaryKeyColumnName.isBlank()) {
            return List.copyOf(columns);
        }

        List<Column> normalized = new ArrayList<>(columns.size());
        for (Column column : columns) {
            if (column.getName().equalsIgnoreCase(primaryKeyColumnName) && !column.isPrimaryKey()) {
                normalized.add(
                    new Column(
                        column.getName(),
                        column.getType(),
                        column.getDeclaredTypeName(),
                        column.getTypeArguments(),
                        false,
                        column.getDefaultValue(),
                        true));
            } else {
                normalized.add(column);
            }
        }
        return normalized;
    }

    public int getTupleLength() {
        return columns.size();
    }

    public List<String> getColumnNames() {
        return columns.stream().map(Column::getName).collect(Collectors.toList());
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(columns.size());
        for (Column col : columns) {
            col.write(out);
        }
    }

    public static Schema read(DataInputStream in) throws IOException {
        int numColumns = in.readInt();
        List<Column> columns = new ArrayList<>(numColumns);
        for (int i = 0; i < numColumns; i++) {
            columns.add(Column.read(in));
        }
        return new Schema(columns);
    }

    public Column getColumn(String columnName) {
        return columns.stream()
            .filter(c -> c.getName().equalsIgnoreCase(columnName))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Column '" + columnName + "' not found in schema."));
    }

    public int getColumnIndex(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Column '" + columnName + "' not found in schema.");
    }
}
