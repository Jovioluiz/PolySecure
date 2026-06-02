package com.polysecure.adapter;

import com.polysecure.adapter.mongodb.MongoAdapter;
import com.polysecure.adapter.neo4j.Neo4jAdapter;
import com.polysecure.adapter.postgres.PostgresAdapter;
import com.polysecure.catalog.StoreConfig;
import org.springframework.stereotype.Component;

@Component
public class AdapterFactory {

    public StoreAdapter create(StoreConfig config) {
        return switch (config.type()) {
            case POSTGRES -> new PostgresAdapter(config);
            case MONGODB  -> new MongoAdapter(config);
            case NEO4J    -> new Neo4jAdapter(config);
        };
    }
}
