package com.polysecure.engine;

import com.polysecure.adapter.AdapterFactory;
import com.polysecure.engine.MaterializedViewCache;
import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.catalog.StoreConfig.StoreType;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.parser.SqlPolyFacade;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class QueryEngineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test").withUsername("test").withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7-jammy");

    static QueryEngine engine;
    static StoreRegistry registry;
    static MaterializedViewCache viewCache;

    @BeforeAll
    static void setup() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id SERIAL PRIMARY KEY, name TEXT NOT NULL, active BOOLEAN DEFAULT TRUE
                )""");
            stmt.execute("INSERT INTO users (name, active) VALUES ('Alice', true), ('Bob', false), ('Carlos', true)");
        }

        try (var mc = com.mongodb.client.MongoClients.create(mongo.getConnectionString())) {
            mc.getDatabase("test").getCollection("profiles").insertMany(List.of(
                new Document("user_id", 1).append("bio", "Developer"),
                new Document("user_id", 3).append("bio", "Designer")
            ));
        }

        AdapterFactory factory = new AdapterFactory();
        MetadataCatalog catalog = new MetadataCatalog();
        registry = new StoreRegistry(factory);

        String[] pgParts = postgres.getJdbcUrl().replace("jdbc:postgresql://", "").split("/");
        String[] hostPort = pgParts[0].split(":");
        registry.register(new StoreConfig("pg", StoreType.POSTGRES,
            hostPort[0], Integer.parseInt(hostPort[1]), "test", "test", "test"));

        registry.register(new StoreConfig("mg", StoreType.MONGODB,
            mongo.getHost(), mongo.getMappedPort(27017), "test", null, null));

        TransactionCoordinator tx = new TransactionCoordinator();
        DdlExecutor ddl = new DdlExecutor(catalog, registry, tx);
        DmlExecutor dml = new DmlExecutor(catalog, registry, tx);
        viewCache = new MaterializedViewCache();
        engine = new QueryEngine(new SqlPolyFacade(), registry, ddl, dml, viewCache);
    }

    @BeforeEach
    void resetCache() {
        viewCache.clear();
    }

    @AfterAll
    static void teardown() {
        registry.unregister("pg");
        registry.unregister("mg");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> select(String sql) {
        return (List<Map<String, Object>>) engine.execute(sql);
    }

    @Test
    void selectAllFromPostgres() {
        assertThat(select("SELECT * FROM pg.users")).hasSize(3);
    }

    @Test
    void selectWithWhereFilter() {
        var rows = select("SELECT * FROM pg.users WHERE active = true");
        assertThat(rows).hasSize(2);
        rows.forEach(r -> assertThat(r.get("active")).isEqualTo(true));
    }

    @Test
    void selectAllFromMongo() {
        assertThat(select("SELECT * FROM mg.profiles")).hasSize(2);
    }

    @Test
    void crossStoreJoin() {
        var rows = select("""
            SELECT u.name, p.bio
            FROM pg.users u
            JOIN mg.profiles p ON u.id = p.user_id
            WHERE u.active = true
            """);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.get("name"))
            .containsExactlyInAnyOrder("Alice", "Carlos");
    }

    @Test
    void selectWithProjection() {
        var rows = select("SELECT u.name FROM pg.users u WHERE u.active = true");
        assertThat(rows).hasSize(2);
        rows.forEach(r -> {
            assertThat(r).containsKey("name");
            assertThat(r).doesNotContainKey("id");
        });
    }

    @Test
    void materialisedViewCachesOnSecondExecution() {
        String sql = "SELECT * FROM mg.profiles";
        select(sql);                                    // 1st — miss, frequency=1, not cached yet
        assertThat(viewCache.viewCount()).isZero();
        select(sql);                                    // 2nd — miss, frequency=2, result cached
        assertThat(viewCache.viewCount()).isOne();
        assertThat(viewCache.stats().get(0).hits()).isZero();  // not yet read from cache
        select(sql);                                    // 3rd — cache hit
        assertThat(viewCache.stats().get(0).hits()).isEqualTo(1);
    }

    @Test
    void dmlInvalidatesCachedView() {
        String sql = "SELECT * FROM pg.users";
        select(sql);  // 1st
        select(sql);  // 2nd — caches
        assertThat(viewCache.viewCount()).isOne();

        // Any write to 'pg' must evict views that depend on it
        viewCache.invalidateByStores(java.util.Set.of("pg"));
        assertThat(viewCache.viewCount()).isZero();
    }

    @Test
    void cachedResultMatchesDirectResult() {
        String sql = "SELECT * FROM pg.users WHERE active = true";
        List<Map<String, Object>> first  = select(sql);  // computed
        List<Map<String, Object>> second = select(sql);  // computed + cached
        List<Map<String, Object>> third  = select(sql);  // served from cache
        assertThat(third).containsExactlyInAnyOrderElementsOf(first);
        assertThat(third).containsExactlyInAnyOrderElementsOf(second);
    }

    @Test
    void bindJoinExcludesOuterRowsWithNoMatch() {
        // PostgreSQL: Alice(1), Bob(2), Carlos(3). MongoDB: profiles for 1 and 3 only.
        // Bob has no matching profile — bind-join must not produce a row for him.
        var rows = select("""
            SELECT u.name, p.bio
            FROM pg.users u
            JOIN mg.profiles p ON u.id = p.user_id
            """);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.get("name"))
            .containsExactlyInAnyOrder("Alice", "Carlos");
        assertThat(rows).extracting(r -> r.get("bio"))
            .containsExactlyInAnyOrder("Developer", "Designer");
    }
}
