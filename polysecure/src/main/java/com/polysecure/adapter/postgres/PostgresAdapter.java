package com.polysecure.adapter.postgres;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class PostgresAdapter implements StoreAdapter {

    private final String storeName;
    private final HikariDataSource dataSource;
    private final JdbcTemplate jdbc;

    public PostgresAdapter(StoreConfig config) {
        this.storeName = config.name();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:postgresql://" + config.host() + ":" + config.port() + "/" + config.database());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(5);
        hikari.setPoolName("ps-" + config.name());
        this.dataSource = new HikariDataSource(hikari);
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        SqlBuilder builder = new SqlBuilder();
        String sql = builder.build(query);
        return jdbc.queryForList(sql, builder.params().toArray());
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()) + (c.primaryKey() ? " PRIMARY KEY" : ""))
            .collect(Collectors.joining(", "));
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" + cols + ")");
    }

    @Override
    public void dropTable(String table) {
        jdbc.execute("DROP TABLE IF EXISTS " + table);
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        String cols = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        jdbc.update("INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")",
            values.values().toArray());
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        SqlBuilder builder = new SqlBuilder();
        String setClause = updates.keySet().stream().map(k -> {
            builder.addParam(updates.get(k));
            return k + " = ?";
        }).collect(Collectors.joining(", "));
        String sql = "UPDATE " + table + " SET " + setClause;
        if (where != null) {
            String whereStr = builder.renderCondition(where, null);
            if (!whereStr.isBlank()) sql += " WHERE " + whereStr;
        }
        return jdbc.update(sql, builder.params().toArray());
    }

    @Override
    public int delete(String table, Condition where) {
        SqlBuilder builder = new SqlBuilder();
        String sql = "DELETE FROM " + table;
        if (where != null) {
            String whereStr = builder.renderCondition(where, null);
            if (!whereStr.isBlank()) sql += " WHERE " + whereStr;
        }
        return jdbc.update(sql, builder.params().toArray());
    }

    @Override
    public void close() { dataSource.close(); }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String toSqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"          -> "INTEGER";
            case "TEXT", "STRING", "VARCHAR" -> "TEXT";
            case "BOOLEAN", "BOOL"         -> "BOOLEAN";
            case "FLOAT", "DOUBLE"         -> "DOUBLE PRECISION";
            case "DOCUMENT"                -> "JSONB";
            default                        -> "TEXT";
        };
    }

    // Package-visible so DdlExecutor/DmlExecutor can reuse the rollback
    static final class SqlBuilder {
        private final List<Object> params = new ArrayList<>();

        void addParam(Object v) { params.add(v); }
        List<Object> params() { return params; }

        String build(LocalSelectQuery query) {
            StringBuilder sb = new StringBuilder("SELECT ");
            if (query.star() || query.projections().isEmpty()) {
                sb.append("*");
            } else {
                sb.append(query.projections().stream()
                    .map(c -> c.tableAlias() != null ? c.tableAlias() + "." + c.column() : c.column())
                    .collect(Collectors.joining(", ")));
            }
            sb.append(" FROM ").append(query.table());
            if (!query.table().equals(query.alias())) sb.append(" ").append(query.alias());
            if (query.where() != null) {
                String w = renderCondition(query.where(), query.alias());
                if (!w.isBlank()) sb.append(" WHERE ").append(w);
            }
            return sb.toString();
        }

        String renderCondition(Condition cond, String alias) {
            return switch (cond) {
                case Condition.And a -> {
                    String l = renderCondition(a.left(), alias);
                    String r = renderCondition(a.right(), alias);
                    yield l.isBlank() || r.isBlank() ? l + r : "(" + l + " AND " + r + ")";
                }
                case Condition.Or o -> {
                    String l = renderCondition(o.left(), alias);
                    String r = renderCondition(o.right(), alias);
                    yield l.isBlank() || r.isBlank() ? "" : "(" + l + " OR " + r + ")";
                }
                case Condition.Compare c -> renderCompare(c, alias);
                case Condition.In i -> renderIn(i);
            };
        }

        private String renderIn(Condition.In i) {
            if (i.values().isEmpty()) return "1=0";
            String field = renderExpr(i.expr());
            String placeholders = i.values().stream()
                .map(v -> { params.add(v); return "?"; })
                .collect(Collectors.joining(", "));
            return field + " IN (" + placeholders + ")";
        }

        private String renderCompare(Condition.Compare c, String alias) {
            if (alias != null && referencesOtherAlias(c, alias)) return "";
            return renderExpr(c.left()) + " " + c.op() + " " + renderExpr(c.right());
        }

        private String renderExpr(Expr e) {
            return switch (e) {
                case Expr.Column col -> col.tableAlias() != null
                    ? col.tableAlias() + "." + col.name() : col.name();
                case Expr.Literal lit -> { params.add(lit.value()); yield "?"; }
                case Expr.Star s -> s.tableAlias() != null ? s.tableAlias() + ".*" : "*";
            };
        }

        private boolean referencesOtherAlias(Condition.Compare c, String alias) {
            return isOther(c.left(), alias) || isOther(c.right(), alias);
        }

        private boolean isOther(Expr e, String alias) {
            return e instanceof Expr.Column col
                && col.tableAlias() != null
                && !col.tableAlias().equals(alias);
        }
    }
}
