package com.polysecure.engine;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Online linear regression cost model — AWESOME-inspired (UCSD, 2021).
 *
 * Features: [bias=1.0, log(outerCardinality+1), numJoins, hasWhere(0/1)]
 * Predicted cost: dot(weights, features) in milliseconds.
 * Weights are updated via gradient descent on each completed SELECT execution.
 *
 * Initial weights are conservative estimates derived from empirical observation
 * of typical polystore query latencies.
 */
@Component
public class LinearCostModel {

    private static final double LEARNING_RATE = 0.001;

    // [bias, log(outerCard), numJoins, hasWhere]
    private final double[] weights = {5.0, 2.0, 10.0, -1.0};

    private final AtomicLong sampleCount = new AtomicLong(0);

    public double predict(double[] features) {
        double sum = 0;
        for (int i = 0; i < Math.min(weights.length, features.length); i++) {
            sum += weights[i] * features[i];
        }
        return Math.max(0, sum);
    }

    public synchronized void update(double[] features, double actualMs) {
        double predicted = predict(features);
        double error = predicted - actualMs;
        for (int i = 0; i < Math.min(weights.length, features.length); i++) {
            weights[i] -= LEARNING_RATE * error * features[i];
        }
        sampleCount.incrementAndGet();
    }

    public synchronized double[] weights() { return weights.clone(); }
    public long sampleCount() { return sampleCount.get(); }
}
