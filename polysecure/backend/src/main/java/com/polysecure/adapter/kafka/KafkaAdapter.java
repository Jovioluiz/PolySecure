/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for Apache Kafka.
 * Each SQL-Poly "table" maps to a Kafka topic.
 * - SELECT: consumes all messages from the beginning of the topic (JSON format)
 * - INSERT: produces a JSON-serialized record
 * - UPDATE/DELETE: not supported by Kafka (append-only) — returns 0
 * - createTable: creates a topic via AdminClient
 * - dropTable: deletes a topic via AdminClient
 *
 * Config: host = bootstrap servers (e.g. localhost:9092), database = default topic (optional)
 */
public class KafkaAdapter implements StoreAdapter {

    private final String storeName;
    private final String bootstrapServers;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaAdapter(StoreConfig config) {
        this.storeName = config.name();
        this.bootstrapServers = config.host() + ":" + config.port();
    }

    @Override public String storeName() { return storeName; }

    // ── SELECT — read all messages from beginning ────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        String topic = query.table();
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "polysecure-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10_000);

        List<Map<String, Object>> results = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 3_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                if (records.isEmpty()) break;
                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> row = parseRecord(record);
                    if (query.where() == null || matchesCondition(query.where(), row)) {
                        results.add(applyProjections(row, query));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Kafka select failed on topic '" + topic + "': " + e.getMessage(), e);
        }
        return results;
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(table, 1, (short) 1))).all().get();
        } catch (Exception e) {
            // Topic may already exist
            if (!e.getMessage().contains("TopicExistsException") && !e.getMessage().contains("TOPIC_ALREADY_EXISTS"))
                throw new RuntimeException("Kafka createTopic failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void dropTable(String table) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.deleteTopics(List.of(table)).all().get();
        } catch (Exception e) {
            throw new RuntimeException("Kafka deleteTopic failed: " + e.getMessage(), e);
        }
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String key = values.containsKey("id") ? String.valueOf(values.get("id")) : null;
            String value = mapper.writeValueAsString(values);
            producer.send(new ProducerRecord<>(table, key, value)).get();
        } catch (Exception e) {
            throw new RuntimeException("Kafka insert failed: " + e.getMessage(), e);
        }
    }

    // UPDATE/DELETE are not supported (Kafka is append-only)
    @Override
    public int update(String table, Map<String, Object> updates, Condition where) { return 0; }

    @Override
    public int delete(String table, Condition where) { return 0; }

    @Override
    public long estimateCardinality(String table) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            var offsets = admin.listOffsets(
                Map.of(new org.apache.kafka.common.TopicPartition(table, 0),
                    org.apache.kafka.clients.admin.OffsetSpec.latest())
            ).all().get();
            return offsets.values().stream().mapToLong(r -> r.offset()).sum();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        return new StoreCapabilities(true, true, false, false, true, false);
    }

    @Override
    public boolean ping() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.describeCluster().nodes().get(3, java.util.concurrent.TimeUnit.SECONDS);
            return true;
        } catch (Exception e) { return false; }
    }

    @Override public void close() {}

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> parseRecord(ConsumerRecord<String, String> record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("_offset", record.offset());
        row.put("_partition", record.partition());
        row.put("_key", record.key());
        try {
            if (record.value() != null && record.value().startsWith("{")) {
                Map<String, Object> parsed = mapper.readValue(record.value(), new TypeReference<>() {});
                row.putAll(parsed);
            } else {
                row.put("value", record.value());
            }
        } catch (Exception e) {
            row.put("value", record.value());
        }
        return row;
    }

    // Simple condition matching for WHERE push-down in consumer loop
    private boolean matchesCondition(Condition cond, Map<String, Object> row) {
        return switch (cond) {
            case Condition.And a     -> matchesCondition(a.left(), row) && matchesCondition(a.right(), row);
            case Condition.Or o      -> matchesCondition(o.left(), row) || matchesCondition(o.right(), row);
            case Condition.Not n     -> !matchesCondition(n.inner(), row);
            case Condition.Compare c -> compareValues(resolveExpr(c.left(), row), c.op(), resolveExpr(c.right(), row));
            case Condition.In i      -> {
                Object v = resolveExpr(i.expr(), row);
                yield v != null && i.values().stream().anyMatch(val -> compareValues(v, "=", val));
            }
            case Condition.IsNull isn -> resolveExpr(isn.expr(), row) == null;
            case Condition.Like like  -> {
                Object v = resolveExpr(like.expr(), row);
                if (v == null) yield false;
                String regex = "(?s)" + like.pattern().replace("%", ".*").replace("_", ".");
                yield v.toString().matches(regex);
            }
            case Condition.Between b -> {
                Object v = resolveExpr(b.expr(), row);
                yield v != null && compareValues(v, ">=", b.low()) && compareValues(v, "<=", b.high());
            }
        };
    }

    private Object resolveExpr(Expr e, Map<String, Object> row) {
        return switch (e) {
            case Expr.Literal lit -> lit.value();
            case Expr.Column col -> row.getOrDefault(col.name(), null);
            case Expr.Star s -> null;
            case Expr.Aggregate ignored -> null;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compareValues(Object left, String op, Object right) {
        if (left == null || right == null) return "=".equals(op) ? left == right : !"=".equals(op);
        if (left instanceof Number l && right instanceof Number r) {
            double ld = l.doubleValue(), rd = r.doubleValue();
            return switch (op) {
                case "=" -> ld == rd; case "!=","<>" -> ld != rd;
                case ">" -> ld > rd; case "<" -> ld < rd;
                case ">=" -> ld >= rd; case "<=" -> ld <= rd; default -> false;
            };
        }
        int cmp = String.valueOf(left).compareTo(String.valueOf(right));
        return switch (op) {
            case "=" -> cmp == 0; case "!=","<>" -> cmp != 0;
            case ">" -> cmp > 0; case "<" -> cmp < 0;
            case ">=" -> cmp >= 0; case "<=" -> cmp <= 0; default -> false;
        };
    }

    private Map<String, Object> applyProjections(Map<String, Object> row, LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return row;
        Map<String, Object> out = new LinkedHashMap<>();
        query.projections().forEach(p -> {
            if (row.containsKey(p.column())) out.put(p.effectiveOutputName(), row.get(p.column()));
        });
        return out;
    }
}
