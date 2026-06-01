package com.polysecure.translator;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.model.ColumnDefinition;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Translates standard PostgreSQL SQL into SQL-Poly format by consulting the MetadataCatalog.
 * If the query cannot be translated (SQL-Poly syntax, unknown table, JSQLParser parse error),
 * the original SQL is returned unchanged so the SQL-Poly engine can handle it.
 */
@Component
public class StandardSqlTranslator {

    private final MetadataCatalog catalog;

    public StandardSqlTranslator(MetadataCatalog catalog) {
        this.catalog = catalog;
    }

    public String translate(String sql) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof PlainSelect plain) {
                return translateSelect(plain);
            }
            return sql;
        } catch (Exception e) {
            return sql;
        }
    }

    private String translateSelect(PlainSelect plain) {
        net.sf.jsqlparser.schema.Table fromTable = extractTable(plain);
        if (fromTable == null) return plain.toString();

        // If already qualified with store prefix (schema name), pass through
        if (fromTable.getSchemaName() != null) return plain.toString();

        String tableName = fromTable.getName();
        if (!catalog.exists(tableName)) return plain.toString();

        List<ColumnDefinition> allColumns = catalog.getColumns(tableName);

        // Group columns by store, preserving definition order
        Map<String, List<ColumnDefinition>> byStore = new LinkedHashMap<>();
        for (ColumnDefinition col : allColumns) {
            byStore.computeIfAbsent(col.store(), k -> new ArrayList<>()).add(col);
        }

        // Single-store table: simple rewrite without JOIN
        if (byStore.size() == 1) {
            String store = byStore.keySet().iterator().next();
            return translateSingleStore(plain, tableName, store, allColumns);
        }

        // Multi-store table: cross-store JOIN
        Map<String, String> storeAliases = new LinkedHashMap<>();
        byStore.keySet().forEach(s -> storeAliases.put(s, s + "_" + tableName.toLowerCase()));

        Optional<ColumnDefinition> pkOpt = catalog.getPrimaryKey(tableName);
        String pkColumn = pkOpt.map(ColumnDefinition::name).orElse("id");
        String primaryStore = pkOpt.map(ColumnDefinition::store).orElse(byStore.keySet().iterator().next());

        StringBuilder sb = new StringBuilder("SELECT ");

        List<SelectItem<?>> items = plain.getSelectItems();
        boolean isStar = items.size() == 1 && items.get(0).toString().trim().equals("*");

        if (isStar) {
            sb.append(allColumns.stream()
                .map(c -> storeAliases.get(c.store()) + "." + c.name())
                .collect(Collectors.joining(", ")));
        } else {
            sb.append(items.stream()
                .map(item -> mapSelectItem(item, allColumns, storeAliases))
                .collect(Collectors.joining(", ")));
        }

        sb.append(" FROM ").append(primaryStore).append(".").append(tableName)
          .append(" ").append(storeAliases.get(primaryStore));

        for (Map.Entry<String, String> e : storeAliases.entrySet()) {
            if (!e.getKey().equals(primaryStore)) {
                sb.append(" JOIN ").append(e.getKey()).append(".").append(tableName)
                  .append(" ").append(e.getValue())
                  .append(" ON ").append(storeAliases.get(primaryStore)).append(".").append(pkColumn)
                  .append(" = ").append(e.getValue()).append(".").append(pkColumn);
            }
        }

        if (plain.getWhere() != null) {
            rewriteExpr(plain.getWhere(), allColumns, storeAliases);
            sb.append(" WHERE ").append(plain.getWhere());
        }

        appendOrderAndLimit(sb, plain, allColumns, storeAliases);

        return sb.toString();
    }

    private String translateSingleStore(PlainSelect plain, String tableName, String store,
                                        List<ColumnDefinition> allColumns) {
        Map<String, String> aliases = Map.of(store, store + "_" + tableName.toLowerCase());
        StringBuilder sb = new StringBuilder("SELECT ");

        List<SelectItem<?>> items = plain.getSelectItems();
        boolean isStar = items.size() == 1 && items.get(0).toString().trim().equals("*");

        if (isStar) {
            sb.append("*");
        } else {
            sb.append(items.stream()
                .map(item -> mapSelectItem(item, allColumns, aliases))
                .collect(Collectors.joining(", ")));
        }

        sb.append(" FROM ").append(store).append(".").append(tableName);

        if (plain.getWhere() != null) {
            sb.append(" WHERE ").append(plain.getWhere());
        }

        appendOrderAndLimit(sb, plain, allColumns, aliases);
        return sb.toString();
    }

    private String mapSelectItem(SelectItem<?> item, List<ColumnDefinition> allColumns,
                                 Map<String, String> storeAliases) {
        Expression expr = item.getExpression();
        if (expr instanceof Column col) {
            ColumnDefinition def = findByName(allColumns, col.getColumnName());
            if (def != null) {
                return storeAliases.get(def.store()) + "." + def.name();
            }
        }
        return item.toString();
    }

    /**
     * Recursively rewrites Column references in the expression tree,
     * prepending the appropriate store alias based on the catalog mapping.
     */
    private void rewriteExpr(Expression expr, List<ColumnDefinition> allColumns,
                              Map<String, String> storeAliases) {
        if (expr == null) return;
        if (expr instanceof Column col) {
            String existing = col.getTable() != null ? col.getTable().getName() : null;
            if (existing != null && storeAliases.containsValue(existing)) return;
            ColumnDefinition def = findByName(allColumns, col.getColumnName());
            if (def != null) col.setTable(new Table(storeAliases.get(def.store())));
        } else if (expr instanceof BinaryExpression bin) {
            rewriteExpr(bin.getLeftExpression(), allColumns, storeAliases);
            rewriteExpr(bin.getRightExpression(), allColumns, storeAliases);
        }
    }

    private void appendOrderAndLimit(StringBuilder sb, PlainSelect plain,
                                     List<ColumnDefinition> allColumns, Map<String, String> storeAliases) {
        if (plain.getOrderByElements() != null && !plain.getOrderByElements().isEmpty()) {
            String order = plain.getOrderByElements().stream().map(elem -> {
                rewriteExpr(elem.getExpression(), allColumns, storeAliases);
                return elem.toString();
            }).collect(Collectors.joining(", "));
            sb.append(" ORDER BY ").append(order);
        }
        if (plain.getLimit() != null) {
            sb.append(" LIMIT ").append(plain.getLimit().getRowCount());
        }
    }

    private net.sf.jsqlparser.schema.Table extractTable(PlainSelect plain) {
        if (plain.getFromItem() instanceof net.sf.jsqlparser.schema.Table t) return t;
        return null;
    }

    private ColumnDefinition findByName(List<ColumnDefinition> cols, String name) {
        return cols.stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
