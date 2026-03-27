package com.indolyn.rill.core.model;

import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Getter
public class Column {
    private final String name;
    private final DataType type;
    private final String declaredTypeName;
    private final List<Integer> typeArguments;
    private final boolean nullable;
    private final String defaultValue;
    private final boolean primaryKey;

    public Column(String name, DataType type) {
        this(name, type, type.name(), List.of(), true, null, false);
    }

    public Column(String name, DataType type, String declaredTypeName, List<Integer> typeArguments) {
        this(name, type, declaredTypeName, typeArguments, true, null, false);
    }

    public Column(
        String name,
        DataType type,
        String declaredTypeName,
        List<Integer> typeArguments,
        boolean nullable,
        String defaultValue,
        boolean primaryKey) {
        this.name = name;
        this.type = type;
        this.declaredTypeName = declaredTypeName == null ? type.name() : declaredTypeName;
        this.typeArguments = List.copyOf(typeArguments == null ? List.of() : typeArguments);
        this.primaryKey = primaryKey;
        this.nullable = primaryKey ? false : nullable;
        this.defaultValue = defaultValue;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(type.name());
        out.writeUTF(declaredTypeName);
        out.writeInt(typeArguments.size());
        for (Integer argument : typeArguments) {
            out.writeInt(argument);
        }
        out.writeBoolean(nullable);
        out.writeBoolean(primaryKey);
        out.writeBoolean(defaultValue != null);
        if (defaultValue != null) {
            out.writeUTF(defaultValue);
        }
    }

    public static Column read(DataInputStream in) throws IOException {
        String name = in.readUTF();
        DataType type = DataType.valueOf(in.readUTF());
        String declaredTypeName = in.readUTF();
        int argumentCount = in.readInt();
        List<Integer> arguments = new ArrayList<>();
        for (int i = 0; i < argumentCount; i++) {
            arguments.add(in.readInt());
        }
        boolean nullable = in.readBoolean();
        boolean primaryKey = in.readBoolean();
        String defaultValue = in.readBoolean() ? in.readUTF() : null;
        return new Column(name, type, declaredTypeName, arguments, nullable, defaultValue, primaryKey);
    }

    public boolean hasLengthLimit() {
        return (type == DataType.VARCHAR || type == DataType.CHAR) && !typeArguments.isEmpty();
    }

    public int getLengthLimit() {
        if (!hasLengthLimit()) {
            return -1;
        }
        return typeArguments.get(0);
    }

    public boolean hasNumericPrecision() {
        return type == DataType.DECIMAL && !typeArguments.isEmpty();
    }

    public int getNumericPrecision() {
        if (!hasNumericPrecision()) {
            return -1;
        }
        return typeArguments.get(0);
    }

    public int getNumericScale() {
        if (!hasNumericPrecision() || typeArguments.size() < 2) {
            return 0;
        }
        return typeArguments.get(1);
    }

    public String formatTypeDeclaration() {
        if (typeArguments.isEmpty()) {
            return declaredTypeName;
        }
        return declaredTypeName + "(" + joinTypeArguments() + ")";
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public String formatCatalogDefinition() {
        StringBuilder builder = new StringBuilder(formatTypeDeclaration());
        if (!nullable) {
            builder.append("||nullable=false");
        }
        if (primaryKey) {
            builder.append("||primaryKey=true");
        }
        if (defaultValue != null) {
            builder.append("||default=")
                .append(Base64.getEncoder().encodeToString(defaultValue.getBytes(StandardCharsets.UTF_8)));
        }
        return builder.toString();
    }

    public static Column fromCatalogDefinition(String name, String definition) {
        String[] definitionParts = (definition == null ? "" : definition).split("\\|\\|");
        String typeDefinition = definitionParts.length == 0 ? "" : definitionParts[0];
        boolean nullable = true;
        boolean primaryKey = false;
        String defaultValue = null;
        for (int i = 1; i < definitionParts.length; i++) {
            String part = definitionParts[i];
            if (part.equalsIgnoreCase("nullable=false")) {
                nullable = false;
            } else if (part.equalsIgnoreCase("primaryKey=true")) {
                primaryKey = true;
                nullable = false;
            } else if (part.startsWith("default=")) {
                defaultValue =
                    new String(
                        Base64.getDecoder().decode(part.substring("default=".length())),
                        StandardCharsets.UTF_8);
            }
        }
        String normalizedDefinition = typeDefinition.trim();
        int argumentStart = normalizedDefinition.indexOf('(');
        String declaredTypeName =
            argumentStart >= 0 ? normalizedDefinition.substring(0, argumentStart).trim() : normalizedDefinition;
        List<Integer> arguments = new ArrayList<>();
        if (argumentStart >= 0 && normalizedDefinition.endsWith(")")) {
            String argumentText =
                normalizedDefinition.substring(argumentStart + 1, normalizedDefinition.length() - 1).trim();
            if (!argumentText.isEmpty()) {
                for (String argument : argumentText.split(",")) {
                    arguments.add(Integer.parseInt(argument.trim()));
                }
            }
        }
        DataType type = DataType.valueOf(declaredTypeName.toUpperCase(Locale.ROOT).replace(' ', '_'));
        return new Column(
            name,
            type,
            declaredTypeName.toUpperCase(Locale.ROOT),
            arguments,
            nullable,
            defaultValue,
            primaryKey);
    }

    private String joinTypeArguments() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < typeArguments.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(typeArguments.get(i));
        }
        return builder.toString();
    }

    public boolean supportsDecimalValue(BigDecimal value) {
        if (!hasNumericPrecision() || value == null) {
            return true;
        }

        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }

        int scale = normalized.scale();
        int precision = normalized.precision();
        int integerDigits = Math.max(precision - scale, 0);
        int allowedScale = getNumericScale();
        int allowedIntegerDigits = getNumericPrecision() - allowedScale;

        return scale <= allowedScale && integerDigits <= allowedIntegerDigits;
    }
}
