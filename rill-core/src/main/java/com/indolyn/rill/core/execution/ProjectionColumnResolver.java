package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.model.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class ProjectionColumnResolver {
    List<Integer> resolve(Schema childSchema, Schema outputSchema) {
        List<Integer> columnIndexes = new ArrayList<>();
        for (String columnName :
            outputSchema.getColumns().stream().map(column -> column.getName()).collect(Collectors.toList())) {
            for (int i = 0; i < childSchema.getColumns().size(); i++) {
                if (childSchema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                    columnIndexes.add(i);
                    break;
                }
            }
        }
        return columnIndexes;
    }
}
