package com.polysecure.engine;

import com.polysecure.model.SelectStatement;
import com.polysecure.model.TableRef;
import org.springframework.stereotype.Component;

/**
 * Assembles query features from StatsRegistry and drives the LinearCostModel.
 * Called by QueryEngine to (a) predict execution cost before planning and
 * (b) record actual latency after execution to keep weights up to date.
 */
@Component
public class CostEstimator {

    private final StatsRegistry stats;
    private final LinearCostModel model;

    public CostEstimator(StatsRegistry stats, LinearCostModel model) {
        this.stats = stats;
        this.model = model;
    }

    public double predict(SelectStatement stmt) {
        return model.predict(features(stmt));
    }

    public void recordExecution(SelectStatement stmt, long actualMs) {
        model.update(features(stmt), actualMs);
    }

    public long cardinality(String storeName, String table) {
        return stats.cardinality(storeName, table);
    }

    public StatsRegistry stats() { return stats; }
    public LinearCostModel model() { return model; }

    // ── helpers ───────────────────────────────────────────────────────────────

    private double[] features(SelectStatement stmt) {
        TableRef from = stmt.from();
        String outerStore = from.store() != null ? from.store() : from.table();
        long outerCard = stats.cardinality(outerStore, from.table());
        int numJoins = stmt.joins().size();
        int hasWhere = stmt.where() != null ? 1 : 0;
        return new double[]{1.0, Math.log1p(outerCard), numJoins, hasWhere};
    }
}
