package com.polysecure.model;

public record ColumnRef(String tableAlias, String column, String outputAlias) {

    public String effectiveOutputName() {
        return outputAlias != null ? outputAlias : column;
    }

    public boolean isStar() {
        return "*".equals(column);
    }
}
