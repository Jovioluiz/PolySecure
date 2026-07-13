/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;

// Every store connection (JDBC, Cassandra, MongoDB, Neo4j...) is managed manually by
// StoreAdapter implementations via StoreRegistry — Spring Boot's own auto-configured
// clients are never injected anywhere, so they're excluded. Left enabled, they try to
// eagerly connect to localhost defaults at startup (e.g. Cassandra's CqlSession.build()
// blocks and fails the whole context if nothing is listening on :9042).
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    CassandraAutoConfiguration.class,
    MongoAutoConfiguration.class,
    Neo4jAutoConfiguration.class
})
public class PolySecureApplication {
    public static void main(String[] args) {
        SpringApplication.run(PolySecureApplication.class, args);
    }
}
