/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.catalog;

public record StoreConfig(
    String name,
    StoreType type,
    String host,
    int port,
    String database,
    String username,
    String password
) {
    public enum StoreType {
        // Original
        POSTGRES, MONGODB, NEO4J,
        // JDBC relational
        MYSQL, MARIADB, SQLSERVER, ORACLE, SNOWFLAKE, SQLITE, FIREBIRD,
        // Analytics / cloud warehouse
        DATABRICKS, DOLPHINDB,
        // NoSQL / Search / Streaming
        ELASTICSEARCH, REDIS, CASSANDRA, SOLR, KAFKA
    }
}
