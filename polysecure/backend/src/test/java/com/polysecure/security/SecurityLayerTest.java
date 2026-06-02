package com.polysecure.security;

import com.polysecure.api.dto.LoginRequest;
import com.polysecure.api.dto.LoginResponse;
import com.polysecure.api.dto.CreateUserRequest;
import com.polysecure.engine.QueryEngine;
import com.polysecure.model.*;
import com.polysecure.security.rbac.UserRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityLayerTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired UserRegistry userRegistry;
    @MockBean QueryEngine engine;

    private static final SelectStatement SELECT_PG = new SelectStatement(
        true, List.of(),
        new TableRef("pg", "users", null),
        List.of(), null
    );

    private static final InsertStatement INSERT_PG = new InsertStatement(
        "orders",
        List.of(new StoreInsertClause("pg", List.of("id"), List.of(1.0)))
    );

    @BeforeEach
    void setup() {
        when(engine.parse(anyString())).thenReturn(SELECT_PG);
        when(engine.execute(anyString())).thenReturn(List.of(
            Map.of("id", 1, "nome", "Alice", "cpf", "12345678901", "email", "alice@example.com")
        ));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String loginAndGetToken(String username, String password) {
        var body = new LoginRequest(username, password);
        var response = rest.postForEntity(url("/auth/login"), body, LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().token();
    }

    private HttpHeaders bearerHeaders(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ── Auth tests ───────────────────────────────────────────────────────────

    @Test
    void loginWithValidCredentials_returnsJwt() {
        var response = rest.postForEntity(
            url("/auth/login"),
            new LoginRequest("admin", "admin123"),
            LoginResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().role()).isEqualTo("dba_admin");
    }

    @Test
    void loginWithWrongPassword_returns401() {
        var response = rest.postForEntity(
            url("/auth/login"),
            new LoginRequest("admin", "wrong"),
            String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Unauthenticated access ────────────────────────────────────────────────

    @Test
    void queryWithoutToken_returns401() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>("{\"sql\":\"SELECT * FROM pg.users\"}", headers);
        var response = rest.exchange(url("/query"), HttpMethod.POST, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Admin (dba_admin) tests ───────────────────────────────────────────────

    @Test
    void adminCanExecuteSelectQuery() {
        String token = loginAndGetToken("admin", "admin123");
        var entity = new HttpEntity<>("{\"sql\":\"SELECT * FROM pg.users\"}", bearerHeaders(token));
        var response = rest.exchange(url("/query"), HttpMethod.POST, entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminCanCreateUser() {
        String token = loginAndGetToken("admin", "admin123");
        var req = new CreateUserRequest("analyst_test", "pass123", "analista_dados");
        var entity = new HttpEntity<>(req, bearerHeaders(token));
        var response = rest.exchange(url("/admin/users"), HttpMethod.POST, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminCanViewAuditLog() {
        String token = loginAndGetToken("admin", "admin123");
        var entity = new HttpEntity<>(null, bearerHeaders(token));
        var response = rest.exchange(url("/admin/audit"), HttpMethod.GET, entity, List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── RBAC enforcement ─────────────────────────────────────────────────────

    @Test
    void analystaCannotInsert_returns403() {
        userRegistry.register("analista_rbac", "pass123", "analista_dados");
        String token = loginAndGetToken("analista_rbac", "pass123");

        // Mock engine to return InsertStatement so permission evaluator sees INSERT op
        when(engine.parse(anyString())).thenReturn(INSERT_PG);

        var entity = new HttpEntity<>(
            "{\"sql\":\"INSERT INTO POLYSTORE orders pg (id) VALUES (1)\"}",
            bearerHeaders(token)
        );
        var response = rest.exchange(url("/query"), HttpMethod.POST, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("FORBIDDEN");
    }

    @Test
    void analystaSelectAppliesAnonymization() {
        userRegistry.register("analista_anon", "pass123", "analista_dados");
        String token = loginAndGetToken("analista_anon", "pass123");

        // SELECT_PG is already set up in @BeforeEach
        var entity = new HttpEntity<>("{\"sql\":\"SELECT * FROM pg.users\"}", bearerHeaders(token));
        var response = rest.exchange(url("/query"), HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) response.getBody().get("rows");
        assertThat(rows).isNotEmpty();
        // CPF should be hashed (64-char hex), not the original value
        assertThat(rows.get(0).get("cpf").toString()).hasSize(64);
        // email should be truncated
        assertThat(rows.get(0).get("email").toString()).doesNotContain("@example.com");
        // nome should be pseudonymized
        assertThat(rows.get(0).get("nome").toString()).startsWith("Usr_");
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints_returns403() {
        userRegistry.register("analista_admin_test", "pass123", "analista_dados");
        String token = loginAndGetToken("analista_admin_test", "pass123");
        var entity = new HttpEntity<>(null, bearerHeaders(token));
        var response = rest.exchange(url("/admin/audit"), HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
