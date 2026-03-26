package com.indolyn.rill.core.sql.ast.type;

import com.indolyn.rill.core.sql.ast.AstNode;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public record TypeReferenceNode(List<String> nameParts, List<Integer> arguments) implements AstNode {

    public TypeReferenceNode {
        nameParts = List.copyOf(nameParts);
        arguments = List.copyOf(arguments);
    }

    public String displayName() {
        return String.join(" ", nameParts);
    }

    public String normalizedName() {
        return nameParts.stream()
            .map(part -> part.toUpperCase(Locale.ROOT))
            .collect(Collectors.joining(" "));
    }
}
