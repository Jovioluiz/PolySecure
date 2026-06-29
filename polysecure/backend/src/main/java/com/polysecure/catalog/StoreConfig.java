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
        MYSQL, MARIADB, SQLSERVER, ORACLE, SNOWFLAKE, CLICKHOUSE,
        // NoSQL / Search / Streaming
        ELASTICSEARCH, REDIS, CASSANDRA, SOLR, KAFKA
    }
}
