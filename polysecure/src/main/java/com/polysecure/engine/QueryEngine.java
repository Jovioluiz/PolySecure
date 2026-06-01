package com.polysecure.engine;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.*;
import com.polysecure.parser.SqlPolyFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueryEngine {

    private static final Logger log = LoggerFactory.getLogger(QueryEngine.class);

    private final SqlPolyFacade parser;
    private final StoreRegistry registry;
    private final DdlExecutor ddl;
    private final DmlExecutor dml;
    private final MaterializedViewCache viewCache;
    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    public QueryEngine(SqlPolyFacade parser, StoreRegistry registry,
                       DdlExecutor ddl, DmlExecutor dml, MaterializedViewCache viewCache) {
        this.parser = parser;
        this.registry = registry;
        this.ddl = ddl;
        this.dml = dml;
        this.viewCache = viewCache;
    }

    public Statement parse(String sql) {
        return parser.parse(sql);
    }

    public Object execute(String sql) {
        String key = normalizeKey(sql);
        Statement stmt = parser.parse(sql);
        return switch (stmt) {
            case SelectStatement s -> {
                // Cache check
                Optional<List<Map<String, Object>>> cached = viewCache.get(key);
                if (cached.isPresent()) {
                    log.debug("materialized view hit: {}", key);
                    yield cached.get();
                }
                List<Map<String, Object>> rows = executeSelect(s);
                // Materialize on reaching the frequency threshold
                if (viewCache.recordExecution(key)) {
                    viewCache.put(key, rows, extractStores(s));
                    log.debug("materialized view created: stores={}", extractStores(s));
                }
                yield rows;
            }
            case InsertStatement i -> {
                DmlResult r = dml.insert(i);
                viewCache.invalidateByStores(r.affectedByStore().keySet());
                yield r;
            }
            case InsertSelectStatement is -> {
                List<Map<String, Object>> rows = executeSelect(is.source());
                DmlResult r = dml.insertFromSelect(is, rows);
                viewCache.invalidateByStores(r.affectedByStore().keySet());
                yield r;
            }
            case UpdateStatement u -> {
                DmlResult r = dml.update(u);
                viewCache.invalidateByStores(r.affectedByStore().keySet());
                yield r;
            }
            case DeleteStatement d -> {
                DmlResult r = dml.delete(d);
                viewCache.invalidateByStores(r.affectedByStore().keySet());
                yield r;
            }
            case CreateTableStatement c -> {
                DmlResult r = ddl.createTable(c);
                viewCache.invalidateByStores(r.affectedByStore().keySet());
                yield r;
            }
            case DropTableStatement d -> {
                DmlResult r = ddl.dropTable(d);
                viewCache.invalidateByStores(r.affectedByStore().keySet());
                yield r;
            }
        };
    }

    // ── SELECT ───────────────────────────────────────────────────────────────

    public List<Map<String, Object>> executeSelect(SelectStatement stmt) {
        validateStores(stmt);
        List<Map<String, Object>> result = executeTable(stmt.from(), stmt.star(), stmt.projections(), stmt.where(), null);
        for (JoinClause join : stmt.joins()) {
            String innerAlias = join.table().effectiveAlias();
            JoinKey key = extractEquiJoinKey(join.on(), innerAlias);
            if (key != null && !result.isEmpty()) {
                // Bind-join: collect distinct outer values, push IN predicate to inner store
                Set<Object> outerValues = result.stream()
                    .map(row -> row.get(key.outerRowKey()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                log.debug("bind-join: {} distinct '{}' values → inner alias '{}'",
                    outerValues.size(), key.outerRowKey(), innerAlias);
                Condition inCond = new Condition.In(
                    new Expr.Column(null, key.innerField()),
                    new ArrayList<>(outerValues)
                );
                List<Map<String, Object>> right = executeTable(
                    join.table(), stmt.star(), stmt.projections(), stmt.where(), inCond);
                result = hashJoin(result, right, key.outerRowKey(), key.innerField());
            } else {
                // Fallback: nested-loop for non-equi joins or empty outer table
                log.debug("nested-loop join for alias '{}' (no equi-join key or empty outer)", innerAlias);
                List<Map<String, Object>> right = executeTable(
                    join.table(), stmt.star(), stmt.projections(), stmt.where(), null);
                result = nestedLoopJoin(result, right, join.on());
            }
        }
        if (stmt.where() != null) {
            result = result.stream()
                .filter(row -> evaluator.evaluate(stmt.where(), row))
                .collect(Collectors.toList());
        }
        return project(result, stmt.star(), stmt.projections());
    }

    // ── Bind-join ────────────────────────────────────────────────────────────

    private record JoinKey(String outerRowKey, String innerField) {}

    /**
     * Extracts the join key from an ON condition of the form outerAlias.col = innerAlias.col.
     * Returns null for non-equi joins or when the condition doesn't reference two columns.
     */
    private JoinKey extractEquiJoinKey(Condition on, String innerAlias) {
        if (!(on instanceof Condition.Compare c)) return null;
        if (!"=".equals(c.op())) return null;
        if (!(c.left() instanceof Expr.Column l) || !(c.right() instanceof Expr.Column r)) return null;
        if (innerAlias.equals(r.tableAlias())) {
            String outerKey = (l.tableAlias() != null ? l.tableAlias() + "." : "") + l.name();
            return new JoinKey(outerKey, r.name());
        }
        if (innerAlias.equals(l.tableAlias())) {
            String outerKey = (r.tableAlias() != null ? r.tableAlias() + "." : "") + r.name();
            return new JoinKey(outerKey, l.name());
        }
        return null;
    }

    private List<Map<String, Object>> hashJoin(List<Map<String, Object>> left,
            List<Map<String, Object>> right, String outerRowKey, String innerField) {
        // Index right side by innerField (handles alias-prefixed keys like "p.user_id")
        Map<String, List<Map<String, Object>>> index = new HashMap<>();
        for (Map<String, Object> r : right) {
            Object val = findValue(r, innerField);
            if (val != null) index.computeIfAbsent(toIndexKey(val), k -> new ArrayList<>()).add(r);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> l : left) {
            Object val = l.get(outerRowKey);
            if (val == null) continue;
            for (Map<String, Object> r : index.getOrDefault(toIndexKey(val), List.of())) {
                Map<String, Object> merged = new LinkedHashMap<>(l);
                merged.putAll(r);
                result.add(merged);
            }
        }
        return result;
    }

    /** Finds a value in a row by bare field name, ignoring any alias prefix. */
    private Object findValue(Map<String, Object> row, String field) {
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String k = e.getKey();
            int dot = k.indexOf('.');
            if ((dot >= 0 && k.substring(dot + 1).equals(field)) || k.equals(field)) return e.getValue();
        }
        return null;
    }

    /**
     * Normalises a join-key value to a canonical string for hash-map lookup.
     * Integers from different stores may differ in type (Integer vs Long),
     * so all integer-like numbers collapse to the same key.
     */
    private String toIndexKey(Object v) {
        if (v instanceof Integer i) return "i:" + i.longValue();
        if (v instanceof Long l)    return "i:" + l;
        if (v instanceof Short s)   return "i:" + s.longValue();
        if (v instanceof Float f)   return "f:" + f.doubleValue();
        if (v instanceof Double d)  return "f:" + d;
        return "s:" + v;
    }

    // ── Table fetch ──────────────────────────────────────────────────────────

    private List<Map<String, Object>> executeTable(TableRef ref, boolean star,
            List<ColumnRef> projections, Condition where, Condition extra) {
        String storeName = ref.store() != null ? ref.store() : ref.table();
        StoreAdapter adapter = registry.get(storeName);
        String alias = ref.effectiveAlias();

        List<ColumnRef> localCols = star ? List.of()
            : projections.stream()
                .filter(c -> c.tableAlias() == null || c.tableAlias().equals(alias))
                .collect(Collectors.toList());

        // Combine the global WHERE with any bind-join IN predicate for this store
        Condition combined = (where == null) ? extra
            : (extra == null) ? where
            : new Condition.And(where, extra);

        List<Map<String, Object>> rows = adapter.select(
            new LocalSelectQuery(storeName, ref.table(), alias, star, localCols, combined));

        return rows.stream().map(row -> {
            Map<String, Object> prefixed = new LinkedHashMap<>();
            row.forEach((k, v) -> prefixed.put(alias + "." + k, v));
            return prefixed;
        }).collect(Collectors.toList());
    }

    // ── Fallback: nested-loop join for non-equi joins ────────────────────────

    private List<Map<String, Object>> nestedLoopJoin(List<Map<String, Object>> left,
                                                       List<Map<String, Object>> right,
                                                       Condition on) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> l : left) {
            for (Map<String, Object> r : right) {
                Map<String, Object> merged = new LinkedHashMap<>(l);
                merged.putAll(r);
                if (on == null || evaluator.evaluate(on, merged)) result.add(merged);
            }
        }
        return result;
    }

    // ── Projection ───────────────────────────────────────────────────────────

    private List<Map<String, Object>> project(List<Map<String, Object>> rows, boolean star,
                                               List<ColumnRef> projections) {
        if (star || projections.isEmpty()) {
            return rows.stream().map(row -> {
                Map<String, Object> clean = new LinkedHashMap<>();
                row.forEach((k, v) -> {
                    int dot = k.indexOf('.');
                    clean.put(dot >= 0 ? k.substring(dot + 1) : k, v);
                });
                return clean;
            }).collect(Collectors.toList());
        }
        return rows.stream().map(row -> {
            Map<String, Object> out = new LinkedHashMap<>();
            for (ColumnRef col : projections) {
                if (col.isStar()) {
                    String prefix = col.tableAlias() + ".";
                    row.forEach((k, v) -> { if (k.startsWith(prefix)) out.put(k.substring(prefix.length()), v); });
                } else {
                    String key = col.tableAlias() != null
                        ? col.tableAlias() + "." + col.column()
                        : findKey(row, col.column());
                    if (key != null && row.containsKey(key)) out.put(col.effectiveOutputName(), row.get(key));
                }
            }
            return out;
        }).collect(Collectors.toList());
    }

    private String findKey(Map<String, Object> row, String column) {
        for (String k : row.keySet()) {
            int dot = k.indexOf('.');
            if (dot >= 0 && k.substring(dot + 1).equals(column)) return k;
            if (k.equals(column)) return k;
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Collapses whitespace so `SELECT  *  FROM x` and `SELECT * FROM x` share the same cache key. */
    private String normalizeKey(String sql) {
        return sql.trim().replaceAll("\\s+", " ");
    }

    /** Extracts the set of store names referenced by a SELECT statement. */
    private Set<String> extractStores(SelectStatement stmt) {
        Set<String> stores = new LinkedHashSet<>();
        TableRef from = stmt.from();
        stores.add(from.store() != null ? from.store() : from.table());
        for (JoinClause join : stmt.joins()) {
            TableRef t = join.table();
            stores.add(t.store() != null ? t.store() : t.table());
        }
        return stores;
    }

    private void validateStores(SelectStatement stmt) {
        List<TableRef> refs = new ArrayList<>();
        refs.add(stmt.from());
        stmt.joins().forEach(j -> refs.add(j.table()));
        refs.forEach(ref -> {
            String name = ref.store() != null ? ref.store() : ref.table();
            if (!registry.exists(name)) throw new IllegalArgumentException("Store not registered: '" + name + "'");
        });
    }

    /** Exposes the cache for admin inspection. */
    public MaterializedViewCache viewCache() { return viewCache; }
}
