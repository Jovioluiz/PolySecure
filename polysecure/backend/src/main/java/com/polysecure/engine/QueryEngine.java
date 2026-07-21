/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.config.PersistenceService;
import com.polysecure.model.*;
import com.polysecure.parser.SqlPolyFacade;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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
    private final CostEstimator cost;
    private final SemanticValidator semanticValidator;
    private final PersistenceService persistence;
    private final ObservationRegistry observationRegistry;
    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    public QueryEngine(SqlPolyFacade parser, StoreRegistry registry,
                       DdlExecutor ddl, DmlExecutor dml,
                       MaterializedViewCache viewCache, CostEstimator cost,
                       SemanticValidator semanticValidator, PersistenceService persistence,
                       ObservationRegistry observationRegistry) {
        this.parser = parser;
        this.registry = registry;
        this.ddl = ddl;
        this.dml = dml;
        this.viewCache = viewCache;
        this.cost = cost;
        this.semanticValidator = semanticValidator;
        this.persistence = persistence;
        this.observationRegistry = observationRegistry;
    }

    public Statement parse(String sql) {
        return parser.parse(sql);
    }

    public Object execute(String sql) {
        String key = normalizeKey(sql);
        Statement stmt = parser.parse(sql);
        semanticValidator.validate(stmt);
        return Observation.createNotStarted("polysecure.query", observationRegistry)
            .lowCardinalityKeyValue("type", stmt.getClass().getSimpleName())
            .observe(() -> dispatch(key, stmt));
    }

    private Object dispatch(String key, Statement stmt) {
        return switch (stmt) {
            case SelectStatement s -> {
                Optional<List<Map<String, Object>>> cached = viewCache.get(key);
                if (cached.isPresent()) {
                    log.debug("materialized view hit: {}", key);
                    yield cached.get();
                }
                long t0 = System.currentTimeMillis();
                List<Map<String, Object>> rows = executeSelect(s);
                long elapsed = System.currentTimeMillis() - t0;
                cost.recordExecution(s, elapsed);
                log.debug("SELECT executed in {}ms (predicted={}ms)", elapsed,
                    String.format("%.1f", cost.predict(s)));
                if (viewCache.recordExecution(key)) {
                    viewCache.put(key, rows, extractStores(s));
                    log.debug("materialized view created: stores={}", extractStores(s));
                }
                yield rows;
            }
            case UnionStatement u -> {
                List<Map<String, Object>> left = executeSelect(u.left());
                List<Map<String, Object>> right = executeSelect(u.right());
                if (u.all()) {
                    List<Map<String, Object>> combined = new ArrayList<>(left);
                    combined.addAll(right);
                    yield combined;
                }
                Set<Map<String, Object>> seen = new LinkedHashSet<>(left);
                seen.addAll(right);
                yield new ArrayList<>(seen);
            }
            case ExceptStatement e -> {
                List<Map<String, Object>> left = executeSelect(e.left());
                List<Map<String, Object>> right = executeSelect(e.right());
                Set<Map<String, Object>> rightSet = new HashSet<>(right);
                yield left.stream().filter(r -> !rightSet.contains(r)).collect(Collectors.toList());
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
            case CreateIndexStatement ci -> ddl.createIndex(ci);
            case AlterTableStatement at -> ddl.alterTable(at);
            case RegisterStoreStatement r -> {
                StoreConfig.StoreType type;
                try {
                    type = StoreConfig.StoreType.valueOf(r.storeType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unknown store type: " + r.storeType()
                        + ". Valid types: " + Arrays.toString(StoreConfig.StoreType.values()));
                }
                StoreConfig config = new StoreConfig(r.name(), type, r.host(), r.port(),
                    r.database(), r.username(), r.password());
                registry.register(config);
                persistence.saveStores();
                yield new DmlResult("REGISTER", r.name(), Map.of(r.name(), 0),
                    "Store '" + r.name() + "' registered as " + type);
            }
        };
    }

    // ── SELECT ───────────────────────────────────────────────────────────────

    public List<Map<String, Object>> executeSelect(SelectStatement stmt) {
        // When GROUP BY is present, fetch all columns so aggregation works correctly
        boolean fetchStar = stmt.star() || !stmt.groupBy().isEmpty();

        // Columns referenced only in JOIN ON clauses, GROUP BY, or inside an aggregate's
        // argument (e.g. the "total" in SUM(total)) must still be fetched from each store
        // even when the SELECT list doesn't project them directly — otherwise bind-join key
        // extraction, grouping, and aggregation silently match/compute against nothing.
        Map<String, Set<String>> joinCols = collectRequiredColumns(stmt);

        List<Map<String, Object>> result = executeTable(stmt.from(), fetchStar, stmt.projections(), stmt.where(), null, joinCols);

        List<JoinClause> joins = orderJoinsByCardinality(stmt);

        for (JoinClause join : joins) {
            String innerAlias = join.table().effectiveAlias();
            JoinKey key = extractEquiJoinKey(join.on(), innerAlias);
            if (key != null && !result.isEmpty()) {
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
                    join.table(), fetchStar, stmt.projections(), stmt.where(), inCond, joinCols);
                result = hashJoin(result, right, key.outerRowKey(), key.innerField());
            } else {
                log.debug("nested-loop join for alias '{}' (no equi-join key or empty outer)", innerAlias);
                List<Map<String, Object>> right = executeTable(
                    join.table(), fetchStar, stmt.projections(), stmt.where(), null, joinCols);
                result = nestedLoopJoin(result, right, join.on());
            }
        }

        // WHERE filter (after joins, before GROUP BY)
        if (stmt.where() != null) {
            result = result.stream()
                .filter(row -> evaluator.evaluate(stmt.where(), row))
                .collect(Collectors.toList());
        }

        // GROUP BY + aggregation + HAVING → replaces normal projection.
        // Also triggered by a bare aggregate with no GROUP BY (e.g. SELECT COUNT(*) FROM t),
        // which standard SQL treats as a single implicit group over the whole result set.
        boolean hasAggregates = stmt.projections().stream().anyMatch(ColumnRef::isAggregate);
        if (!stmt.groupBy().isEmpty() || hasAggregates) {
            result = applyGroupBy(result, stmt.groupBy(), stmt.projections(), stmt.having());
            result = applyOrderBy(result, stmt.orderBy());
            return applyLimit(result, stmt.limit(), stmt.offset());
        }

        // Normal projection
        result = project(result, stmt.star(), stmt.projections());

        // ORDER BY and LIMIT after projection (so aliases are resolved)
        result = applyOrderBy(result, stmt.orderBy());
        return applyLimit(result, stmt.limit(), stmt.offset());
    }

    // ── GROUP BY / Aggregation ────────────────────────────────────────────────

    private List<Map<String, Object>> applyGroupBy(
            List<Map<String, Object>> rows,
            List<Expr> groupByExprs,
            List<ColumnRef> projections,
            Condition having) {

        Map<List<Object>, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        if (groupByExprs.isEmpty()) {
            // Bare aggregate with no GROUP BY: the whole result set is a single implicit
            // group (standard SQL semantics — e.g. SELECT COUNT(*) FROM t always returns
            // exactly one row, even when t is empty).
            groups.put(List.of(), rows);
        } else {
            for (Map<String, Object> row : rows) {
                List<Object> key = groupByExprs.stream()
                    .map(e -> resolveExpr(e, row))
                    .collect(Collectors.toList());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (List<Map<String, Object>> groupRows : groups.values()) {
            Map<String, Object> outputRow = new LinkedHashMap<>();
            for (ColumnRef proj : projections) {
                if (proj.isAggregate()) {
                    Expr.Aggregate agg = (Expr.Aggregate) proj.aggExpr();
                    Object value = computeAggregate(agg, groupRows);
                    outputRow.put(proj.effectiveOutputName(), value);
                    // Store by expression key too, for HAVING COUNT(*) > N style
                    String aggKey = ConditionEvaluator.aggToKey(agg);
                    if (!aggKey.equals(proj.effectiveOutputName())) {
                        outputRow.put(aggKey, value);
                    }
                } else {
                    Object val = groupRows.isEmpty() ? null
                        : resolveExpr(new Expr.Column(proj.tableAlias(), proj.column()), groupRows.get(0));
                    outputRow.put(proj.effectiveOutputName(), val);
                }
            }
            result.add(outputRow);
        }

        if (having != null) {
            result = result.stream()
                .filter(row -> evaluator.evaluate(having, row))
                .collect(Collectors.toList());
        }

        // Strip the internal expression-key duplicates added above (e.g. "AVG(estoque)"
        // alongside the user's "media" alias) — they only exist so HAVING can reference the
        // raw aggregate expression; the client should only ever see the projected names.
        Set<String> visibleNames = projections.stream()
            .map(ColumnRef::effectiveOutputName)
            .collect(Collectors.toSet());
        return result.stream()
            .map(row -> {
                Map<String, Object> clean = new LinkedHashMap<>();
                row.forEach((k, v) -> { if (visibleNames.contains(k)) clean.put(k, v); });
                return clean;
            })
            .collect(Collectors.toList());
    }

    private Object computeAggregate(Expr.Aggregate agg, List<Map<String, Object>> groupRows) {
        return switch (agg.func().toUpperCase()) {
            case "COUNT" -> (long) groupRows.size();
            case "SUM" -> groupRows.stream()
                .map(r -> resolveExpr(agg.arg(), r))
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .sum();
            case "AVG" -> groupRows.stream()
                .map(r -> resolveExpr(agg.arg(), r))
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .average().orElse(0.0);
            case "MIN" -> groupRows.stream()
                .map(r -> resolveExpr(agg.arg(), r))
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(v -> toDouble(v)))
                .orElse(null);
            case "MAX" -> groupRows.stream()
                .map(r -> resolveExpr(agg.arg(), r))
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(v -> toDouble(v)))
                .orElse(null);
            default -> throw new IllegalArgumentException("Unknown aggregate function: " + agg.func()
                + ". Supported: COUNT, SUM, AVG, MIN, MAX");
        };
    }

    // ── ORDER BY ─────────────────────────────────────────────────────────────

    private List<Map<String, Object>> applyOrderBy(List<Map<String, Object>> rows, List<OrderItem> orderBy) {
        if (orderBy == null || orderBy.isEmpty()) return rows;
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> {
            for (OrderItem item : orderBy) {
                Object va = resolveExpr(item.expr(), a);
                Object vb = resolveExpr(item.expr(), b);
                int cmp = compareValues(va, vb);
                if (cmp != 0) return item.desc() ? -cmp : cmp;
            }
            return 0;
        });
        return sorted;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof String sa && b instanceof Number nb) {
            try { return Double.compare(Double.parseDouble(sa), nb.doubleValue()); }
            catch (NumberFormatException ignored) {}
        }
        if (a instanceof Number na && b instanceof String sb) {
            try { return Double.compare(na.doubleValue(), Double.parseDouble(sb)); }
            catch (NumberFormatException ignored) {}
        }
        if (a instanceof Comparable ca && a.getClass().equals(b.getClass())) {
            return ca.compareTo((Comparable) b);
        }
        return a.toString().compareTo(b.toString());
    }

    // ── LIMIT / OFFSET ────────────────────────────────────────────────────────

    private List<Map<String, Object>> applyLimit(List<Map<String, Object>> rows, Integer limit, Integer offset) {
        if (limit == null) return rows;
        int skip = offset != null ? offset : 0;
        return rows.stream().skip(skip).limit(limit).collect(Collectors.toList());
    }

    // ── Expression resolver (used for GROUP BY / ORDER BY key extraction) ─────

    private Object resolveExpr(Expr e, Map<String, Object> row) {
        return switch (e) {
            case Expr.Literal lit -> lit.value();
            case Expr.Star ignored -> null;
            case Expr.Column col -> {
                String qualified = col.tableAlias() != null ? col.tableAlias() + "." + col.name() : null;
                if (qualified != null && row.containsKey(qualified)) yield row.get(qualified);
                if (row.containsKey(col.name())) yield row.get(col.name());
                String suffix = "." + col.name();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getKey().endsWith(suffix)) yield entry.getValue();
                }
                // Case-insensitive fallback: some stores (e.g. SQL Server) report column names
                // in a different case than the query text — the store itself resolves
                // identifiers case-insensitively, but the exact-case matches above won't.
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String k = entry.getKey();
                    if (k.equalsIgnoreCase(col.name())
                        || k.regionMatches(true, k.length() - suffix.length(), suffix, 0, suffix.length())) {
                        yield entry.getValue();
                    }
                }
                yield null;
            }
            case Expr.Aggregate agg -> row.getOrDefault(ConditionEvaluator.aggToKey(agg), null);
        };
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    // ── Phase 4: cardinality-based join ordering ──────────────────────────────

    private List<JoinClause> orderJoinsByCardinality(SelectStatement stmt) {
        if (stmt.joins().isEmpty()) return stmt.joins();
        String fromAlias = stmt.from().effectiveAlias();
        boolean allStar = stmt.joins().stream().allMatch(j -> {
            JoinKey k = extractEquiJoinKey(j.on(), j.table().effectiveAlias());
            return k == null || k.outerRowKey().startsWith(fromAlias + ".");
        });
        if (!allStar) {
            log.debug("join ordering: chain-join detected — keeping original order");
            return stmt.joins();
        }
        List<JoinClause> sorted = stmt.joins().stream()
            .sorted(Comparator.comparingLong(j -> {
                TableRef t = j.table();
                String store = t.store() != null ? t.store() : t.table();
                return cost.cardinality(store, t.table());
            }))
            .collect(Collectors.toList());
        if (!sorted.equals(stmt.joins())) {
            log.debug("join ordering: reordered {} joins by cardinality", sorted.size());
        }
        return sorted;
    }

    // ── Bind-join ────────────────────────────────────────────────────────────

    private record JoinKey(String outerRowKey, String innerField) {}

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

    private Object findValue(Map<String, Object> row, String field) {
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String k = e.getKey();
            int dot = k.indexOf('.');
            if ((dot >= 0 && k.substring(dot + 1).equals(field)) || k.equals(field)) return e.getValue();
        }
        return null;
    }

    private String toIndexKey(Object v) {
        if (v instanceof Number n) return "n:" + n.doubleValue();
        if (v instanceof String s) {
            try { return "n:" + Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return "s:" + v;
    }

    // ── Table fetch ──────────────────────────────────────────────────────────

    private List<Map<String, Object>> executeTable(TableRef ref, boolean star,
            List<ColumnRef> projections, Condition where, Condition extra,
            Map<String, Set<String>> joinCols) {
        String storeName = ref.store() != null ? ref.store() : ref.table();
        StoreAdapter adapter = registry.get(storeName);
        String alias = ref.effectiveAlias();

        // Strip aggregate projections — adapters receive only concrete columns
        List<ColumnRef> nonAgg = star ? List.of()
            : projections.stream().filter(c -> !c.isAggregate()).collect(Collectors.toList());
        boolean effectiveStar = star || nonAgg.isEmpty();
        List<ColumnRef> localCols = effectiveStar ? List.of()
            : buildLocalCols(nonAgg, alias, joinCols.getOrDefault(alias, Set.of()));

        Condition combined = (where == null) ? extra
            : (extra == null) ? where
            : new Condition.And(where, extra);

        List<Map<String, Object>> rows = adapter.select(
            new LocalSelectQuery(storeName, ref.table(), alias, effectiveStar, localCols, combined));

        return rows.stream().map(row -> {
            Map<String, Object> prefixed = new LinkedHashMap<>();
            row.forEach((k, v) -> prefixed.put(alias + "." + k, v));
            return prefixed;
        }).collect(Collectors.toList());
    }

    // Adds any join-key columns for this alias that aren't already in the SELECT list —
    // needed internally for bind-join extraction / hash-join matching, stripped again
    // at final projection time since project() only emits stmt.projections().
    private List<ColumnRef> buildLocalCols(List<ColumnRef> nonAgg, String alias, Set<String> requiredJoinCols) {
        List<ColumnRef> cols = nonAgg.stream()
            .filter(c -> c.tableAlias() == null || c.tableAlias().equals(alias))
            .collect(Collectors.toCollection(ArrayList::new));
        Set<String> existing = cols.stream().map(ColumnRef::column).collect(Collectors.toCollection(HashSet::new));
        for (String col : requiredJoinCols) {
            if (existing.add(col)) cols.add(new ColumnRef(alias, col, col));
        }
        return cols;
    }

    // ── Join-key column discovery ───────────────────────────────────────────────

    private Map<String, Set<String>> collectJoinColumns(SelectStatement stmt) {
        Map<String, Set<String>> result = new HashMap<>();
        for (JoinClause join : stmt.joins()) {
            collectColumnsFromCondition(join.on(), result);
        }
        return result;
    }

    // Extends collectJoinColumns() with columns needed for correct aggregation/grouping
    // that may not appear as plain projections: the column inside an aggregate's argument
    // (e.g. "total" in SUM(total)) and any GROUP BY key column.
    private Map<String, Set<String>> collectRequiredColumns(SelectStatement stmt) {
        Map<String, Set<String>> result = collectJoinColumns(stmt);
        List<String> allAliases = new ArrayList<>();
        allAliases.add(stmt.from().effectiveAlias());
        for (JoinClause j : stmt.joins()) allAliases.add(j.table().effectiveAlias());

        for (ColumnRef proj : stmt.projections()) {
            if (proj.isAggregate() && proj.aggExpr() instanceof Expr.Aggregate agg) {
                addRequiredColumn(agg.arg(), allAliases, result);
            }
        }
        for (Expr e : stmt.groupBy()) {
            addRequiredColumn(e, allAliases, result);
        }
        return result;
    }

    // Qualified columns (alias.col) are attributed to that alias only. Unqualified columns
    // are requested from every table in the query since we can't tell which one owns them —
    // harmless in the common single-table case, and mirrors how buildLocalCols() already
    // treats unqualified plain projections.
    private void addRequiredColumn(Expr e, List<String> allAliases, Map<String, Set<String>> out) {
        if (!(e instanceof Expr.Column col)) return;
        if (col.tableAlias() != null) {
            out.computeIfAbsent(col.tableAlias(), k -> new HashSet<>()).add(col.name());
        } else {
            for (String alias : allAliases) {
                out.computeIfAbsent(alias, k -> new HashSet<>()).add(col.name());
            }
        }
    }

    private void collectColumnsFromCondition(Condition cond, Map<String, Set<String>> out) {
        switch (cond) {
            case Condition.And a -> {
                collectColumnsFromCondition(a.left(), out);
                collectColumnsFromCondition(a.right(), out);
            }
            case Condition.Or o -> {
                collectColumnsFromCondition(o.left(), out);
                collectColumnsFromCondition(o.right(), out);
            }
            case Condition.Not n -> collectColumnsFromCondition(n.inner(), out);
            case Condition.Compare c -> {
                collectColumnFromExpr(c.left(), out);
                collectColumnFromExpr(c.right(), out);
            }
            case Condition.In i -> collectColumnFromExpr(i.expr(), out);
            case Condition.IsNull isn -> collectColumnFromExpr(isn.expr(), out);
            case Condition.Like like -> collectColumnFromExpr(like.expr(), out);
            case Condition.Between b -> collectColumnFromExpr(b.expr(), out);
        }
    }

    private void collectColumnFromExpr(Expr e, Map<String, Set<String>> out) {
        if (e instanceof Expr.Column col && col.tableAlias() != null) {
            out.computeIfAbsent(col.tableAlias(), k -> new HashSet<>()).add(col.name());
        }
    }

    // ── Fallback: nested-loop join for non-equi joins ────────────────────────

    // Guards against silently exhausting the heap when a JOIN can't be pushed down
    // (e.g. ON/WHERE reference a store name that doesn't match the table's effective
    // alias) and both sides end up being unfiltered full-table fetches.
    private static final long MAX_NESTED_LOOP_PAIRS = 500_000L;

    private List<Map<String, Object>> nestedLoopJoin(List<Map<String, Object>> left,
                                                       List<Map<String, Object>> right,
                                                       Condition on) {
        long pairs = (long) left.size() * (long) right.size();
        if (pairs > MAX_NESTED_LOOP_PAIRS) {
            throw new IllegalStateException(
                "Nested-loop join would evaluate " + pairs + " row pairs (" + left.size() + " x " + right.size()
                + "), which exceeds the safety limit of " + MAX_NESTED_LOOP_PAIRS + ". This usually means the "
                + "JOIN could not be pushed down as a bind-join because a column reference in ON/WHERE didn't "
                + "match the table's alias. Add explicit table aliases, e.g. "
                + "'FROM store1.table1 a JOIN store2.table2 b ON a.id = b.id WHERE a.id < 100', "
                + "and qualify columns with those aliases instead of the store names.");
        }
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
                Map<String, Long> nameCount = row.keySet().stream()
                    .collect(Collectors.groupingBy(
                        k -> { int d = k.indexOf('.'); return d >= 0 ? k.substring(d + 1) : k; },
                        Collectors.counting()));
                Map<String, Object> clean = new LinkedHashMap<>();
                row.forEach((k, v) -> {
                    int dot = k.indexOf('.');
                    String bare = dot >= 0 ? k.substring(dot + 1) : k;
                    clean.put(nameCount.get(bare) > 1 ? k.replace('.', '_') : bare, v);
                });
                return clean;
            }).collect(Collectors.toList());
        }
        // When two projections without an explicit AS alias would collide on the same
        // output name (e.g. "s.nome, m.nome" from a cross-store JOIN), disambiguate with
        // "alias_column" instead of silently letting the later one overwrite the former —
        // mirrors the collision handling already done for SELECT *.
        Map<String, Long> autoNameCount = projections.stream()
            .filter(c -> !c.isStar() && c.outputAlias() == null)
            .collect(Collectors.groupingBy(ColumnRef::column, Collectors.counting()));

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
                    if (key != null && row.containsKey(key)) {
                        boolean collides = col.outputAlias() == null && col.tableAlias() != null
                            && autoNameCount.getOrDefault(col.column(), 0L) > 1;
                        String outName = collides ? col.tableAlias() + "_" + col.column() : col.effectiveOutputName();
                        out.put(outName, row.get(key));
                    }
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
        // Case-insensitive fallback — see resolveExpr() for why this is needed.
        for (String k : row.keySet()) {
            int dot = k.indexOf('.');
            if (dot >= 0 && k.substring(dot + 1).equalsIgnoreCase(column)) return k;
            if (k.equalsIgnoreCase(column)) return k;
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String normalizeKey(String sql) {
        return sql.trim().replaceAll("\\s+", " ");
    }

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

    public MaterializedViewCache viewCache() { return viewCache; }
    public CostEstimator costEstimator() { return cost; }
}
