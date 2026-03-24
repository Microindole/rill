package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class QueryResultRenderer {
    String render(TupleIterator iterator) throws IOException {
        if (iterator == null) {
            return "Query OK.";
        }

        List<Tuple> results = collect(iterator);
        Schema schema = iterator.getOutputSchema();
        if (schema != null && schema.getColumnNames().get(0).endsWith("_rows")) {
            return renderAffectedRows(results);
        }
        if (schema == null) {
            return "Query OK.";
        }
        if (results.isEmpty()) {
            return "Query finished, 0 rows returned.";
        }

        List<String> columnNames = schema.getColumnNames();
        List<Integer> columnWidths = calculateColumnWidths(columnNames, results);
        StringBuilder builder = new StringBuilder();
        builder.append(getSeparator(columnWidths)).append("\n");
        builder.append(getRow(columnNames, columnWidths)).append("\n");
        builder.append(getSeparator(columnWidths)).append("\n");

        for (Tuple tuple : results) {
            List<String> values =
                tuple.getValues().stream()
                    .map(value -> value.getValue() == null ? "NULL" : value.getValue().toString())
                    .collect(Collectors.toList());
            builder.append(getRow(values, columnWidths)).append("\n");
        }

        builder.append(getSeparator(columnWidths)).append("\n");
        builder.append("Query finished, ").append(results.size()).append(" rows returned.");
        return builder.toString();
    }

    private List<Tuple> collect(TupleIterator iterator) throws IOException {
        List<Tuple> results = new ArrayList<>();
        while (iterator.hasNext()) {
            Tuple tuple = iterator.next();
            if (tuple != null) {
                results.add(tuple);
            }
        }
        return results;
    }

    private String renderAffectedRows(List<Tuple> results) {
        if (results.isEmpty() || results.get(0).getValues().isEmpty()) {
            return "Query OK, 0 rows affected.";
        }
        int affectedRows = (Integer) results.get(0).getValues().get(0).getValue();
        return "Query OK, " + affectedRows + " rows affected.";
    }

    private List<Integer> calculateColumnWidths(List<String> columnNames, List<Tuple> results) {
        List<Integer> columnWidths = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            int maxWidth = columnNames.get(i).length();
            for (Tuple tuple : results) {
                Object value = tuple.getValues().get(i).getValue();
                String cellValue = value == null ? "NULL" : value.toString();
                maxWidth = Math.max(maxWidth, cellValue.length());
            }
            columnWidths.add(maxWidth);
        }
        return columnWidths;
    }

    private String getRow(List<String> cells, List<Integer> widths) {
        StringBuilder builder = new StringBuilder("|");
        for (int i = 0; i < cells.size(); i++) {
            builder.append(String.format(" %-" + widths.get(i) + "s |", cells.get(i)));
        }
        return builder.toString();
    }

    private String getSeparator(List<Integer> widths) {
        StringBuilder builder = new StringBuilder("+");
        for (Integer width : widths) {
            builder.append("-".repeat(width + 2)).append("+");
        }
        return builder.toString();
    }
}
