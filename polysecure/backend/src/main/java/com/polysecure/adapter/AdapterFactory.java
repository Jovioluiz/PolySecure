package com.polysecure.adapter;

import com.polysecure.adapter.cassandra.CassandraAdapter;
import com.polysecure.adapter.elasticsearch.ElasticsearchAdapter;
import com.polysecure.adapter.jdbc.*;
import com.polysecure.adapter.kafka.KafkaAdapter;
import com.polysecure.adapter.mongodb.MongoAdapter;
import com.polysecure.adapter.neo4j.Neo4jAdapter;
import com.polysecure.adapter.postgres.PostgresAdapter;
import com.polysecure.adapter.redis.RedisAdapter;
import com.polysecure.adapter.solr.SolrAdapter;
import com.polysecure.catalog.StoreConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdapterFactory {

    @Value("${polysecure.adapter.max-rows:1000}")
    private int maxRows;

    public StoreAdapter create(StoreConfig config) {
        return switch (config.type()) {
            // Original adapters
            case POSTGRES       -> new PostgresAdapter(config);
            case MONGODB        -> new MongoAdapter(config);
            case NEO4J          -> new Neo4jAdapter(config);
            // JDBC relational
            case MYSQL          -> new MysqlAdapter(config);
            case MARIADB        -> new MariaDbAdapter(config);
            case SQLSERVER      -> new SqlServerAdapter(config);
            case ORACLE         -> new OracleAdapter(config);
            case SNOWFLAKE      -> new SnowflakeAdapter(config);
            case CLICKHOUSE     -> new ClickHouseAdapter(config);
            // NoSQL / Search / Streaming
            case ELASTICSEARCH  -> new ElasticsearchAdapter(config);
            case REDIS          -> new RedisAdapter(config);
            case CASSANDRA      -> new CassandraAdapter(config);
            case SOLR           -> new SolrAdapter(config, maxRows);
            case KAFKA          -> new KafkaAdapter(config);
        };
    }
}
