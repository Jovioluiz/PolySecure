/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.parser;

import com.polysecure.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SqlPolyParserTest {

    private final SqlPolyFacade facade = new SqlPolyFacade();

    // ── Phase 1: SELECT ──────────────────────────────────────────────────────

    @Test
    void parsesSimpleSingleStoreSelect() {
        var stmt = (SelectStatement) facade.parse("SELECT * FROM postgres.users");
        assertThat(stmt.star()).isTrue();
        assertThat(stmt.from().store()).isEqualTo("postgres");
        assertThat(stmt.from().table()).isEqualTo("users");
        assertThat(stmt.joins()).isEmpty();
        assertThat(stmt.where()).isNull();
    }

    @Test
    void parsesProjectionWithAlias() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT u.name AS nome, u.email FROM postgres.users u");
        assertThat(stmt.projections()).hasSize(2);
        assertThat(stmt.projections().get(0).effectiveOutputName()).isEqualTo("nome");
        assertThat(stmt.from().effectiveAlias()).isEqualTo("u");
    }

    @Test
    void parsesWhereWithBooleanLiteral() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM postgres.users WHERE active = true");
        var cmp = (Condition.Compare) stmt.where();
        assertThat(cmp.op()).isEqualTo("=");
        assertThat(((Expr.Literal) cmp.right()).value()).isEqualTo(true);
    }

    @Test
    void parsesCrossStoreJoin() {
        var stmt = (SelectStatement) facade.parse("""
            SELECT u.name, p.profile
            FROM postgres.users u
            JOIN mongodb.profiles p ON u.id = p.user_id
            WHERE u.active = true
            """);
        assertThat(stmt.from().store()).isEqualTo("postgres");
        assertThat(stmt.joins()).hasSize(1);
        var join = stmt.joins().get(0);
        assertThat(join.table().store()).isEqualTo("mongodb");
        assertThat(((Expr.Column) ((Condition.Compare) join.on()).left()).tableAlias()).isEqualTo("u");
    }

    @Test
    void parsesAndCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM postgres.users WHERE age > 18 AND active = true");
        assertThat(stmt.where()).isInstanceOf(Condition.And.class);
    }

    @Test
    void throwsOnSyntaxError() {
        assertThatThrownBy(() -> facade.parse("SELECT FROM"))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void parsesStringLiteral() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM postgres.users WHERE name = 'João'");
        var cmp = (Condition.Compare) stmt.where();
        assertThat(((Expr.Literal) cmp.right()).value()).isEqualTo("João");
    }

    @Test
    void parsesNumericLiteral() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM postgres.users WHERE id > 100");
        var cmp = (Condition.Compare) stmt.where();
        assertThat(((Expr.Literal) cmp.right()).value()).isEqualTo(100L);
    }

    // ── Phase 2: INSERT ──────────────────────────────────────────────────────

    @Test
    void parsesInsertStatement() {
        var stmt = (InsertStatement) facade.parse("""
            INSERT INTO POLYSTORE orders
                pg (id, total) VALUES (1, 150.0)
                mg (id, details) VALUES (1, 'json')
            """);
        assertThat(stmt.tableName()).isEqualTo("orders");
        assertThat(stmt.clauses()).hasSize(2);
        assertThat(stmt.clauses().get(0).storeName()).isEqualTo("pg");
        assertThat(stmt.clauses().get(0).columns()).containsExactly("id", "total");
        assertThat(stmt.clauses().get(0).values()).containsExactly(1L, 150.0);
    }

    // ── Phase 2: UPDATE ──────────────────────────────────────────────────────

    @Test
    void parsesUpdateStatement() {
        var stmt = (UpdateStatement) facade.parse("""
            UPDATE POLYSTORE orders
                SET pg.status = 'shipped', mg.tracking = 'BR123'
                WHERE id = 1
            """);
        assertThat(stmt.tableName()).isEqualTo("orders");
        assertThat(stmt.sets()).hasSize(2);
        assertThat(stmt.sets().get(0).store()).isEqualTo("pg");
        assertThat(stmt.sets().get(0).column()).isEqualTo("status");
        assertThat(stmt.sets().get(0).value()).isEqualTo("shipped");
        assertThat(stmt.where()).isNotNull();
    }

    // ── Phase 2: DELETE ──────────────────────────────────────────────────────

    @Test
    void parsesDeleteStatement() {
        var stmt = (DeleteStatement) facade.parse(
            "DELETE FROM POLYSTORE orders WHERE id = 1");
        assertThat(stmt.tableName()).isEqualTo("orders");
        assertThat(stmt.where()).isInstanceOf(Condition.Compare.class);
    }

    // ── Phase 2: CREATE TABLE ────────────────────────────────────────────────

    @Test
    void parsesCreateTableStatement() {
        var stmt = (CreateTableStatement) facade.parse("""
            CREATE POLYSTORE TABLE pedidos (
                id INT STORE pg PRIMARY KEY,
                detalhes DOCUMENT STORE mg,
                conexoes GRAPH STORE neo
            )
            """);
        assertThat(stmt.tableName()).isEqualTo("pedidos");
        assertThat(stmt.columns()).hasSize(3);
        assertThat(stmt.columns().get(0).primaryKey()).isTrue();
        assertThat(stmt.columns().get(0).store()).isEqualTo("pg");
        assertThat(stmt.columns().get(1).type()).isEqualTo("DOCUMENT");
        assertThat(stmt.columns().get(2).store()).isEqualTo("neo");
    }

    // ── Phase 2: DROP TABLE ──────────────────────────────────────────────────

    @Test
    void parsesDropTableStatement() {
        var stmt = (DropTableStatement) facade.parse("DROP POLYSTORE TABLE pedidos");
        assertThat(stmt.tableName()).isEqualTo("pedidos");
    }

    // ── INSERT … SELECT ──────────────────────────────────────────────────────

    @Test
    void parsesInsertSelectSingleStore() {
        var stmt = (InsertSelectStatement) facade.parse("""
            INSERT INTO POLYSTORE clientes
              mg (id, nome, email)
            SELECT cust_id AS id, cust_name AS nome, email
            FROM pg.customers
            """);
        assertThat(stmt.tableName()).isEqualTo("clientes");
        assertThat(stmt.targets()).hasSize(1);
        assertThat(stmt.targets().get(0).storeName()).isEqualTo("mg");
        assertThat(stmt.targets().get(0).columns()).containsExactly("id", "nome", "email");
        assertThat(stmt.source().from().store()).isEqualTo("pg");
        assertThat(stmt.source().from().table()).isEqualTo("customers");
        assertThat(stmt.source().projections()).hasSize(3);
        assertThat(stmt.source().projections().get(0).effectiveOutputName()).isEqualTo("id");
        assertThat(stmt.source().projections().get(1).effectiveOutputName()).isEqualTo("nome");
    }

    @Test
    void parsesInsertSelectMultiStore() {
        var stmt = (InsertSelectStatement) facade.parse("""
            INSERT INTO POLYSTORE pedidos
              pg (id, cliente, total)
              mg (id, detalhes)
            SELECT p.cust_id AS id, p.name AS cliente, p.amount AS total, m.notes AS detalhes
            FROM pg.source_orders p JOIN mg.source_details m ON p.cust_id = m.cid
            """);
        assertThat(stmt.tableName()).isEqualTo("pedidos");
        assertThat(stmt.targets()).hasSize(2);
        assertThat(stmt.targets().get(0).storeName()).isEqualTo("pg");
        assertThat(stmt.targets().get(0).columns()).containsExactly("id", "cliente", "total");
        assertThat(stmt.targets().get(1).storeName()).isEqualTo("mg");
        assertThat(stmt.targets().get(1).columns()).containsExactly("id", "detalhes");
        assertThat(stmt.source().joins()).hasSize(1);
    }

    @Test
    void parsesInsertSelectWithWhere() {
        var stmt = (InsertSelectStatement) facade.parse("""
            INSERT INTO POLYSTORE ativos pg (id, nome)
            SELECT id, nome FROM pg.clientes WHERE ativo = true
            """);
        assertThat(stmt.tableName()).isEqualTo("ativos");
        assertThat(stmt.source().where()).isNotNull();
        var cmp = (Condition.Compare) stmt.source().where();
        assertThat(cmp.op()).isEqualTo("=");
        assertThat(((Expr.Literal) cmp.right()).value()).isEqualTo(true);
    }

    // ── New operators ────────────────────────────────────────────────────────

    @Test
    void parsesInCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.orders WHERE id IN (1, 2, 3)");
        var in = (Condition.In) stmt.where();
        assertThat(in.values()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void parsesNotInCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.orders WHERE id NOT IN (5, 6)");
        var not = (Condition.Not) stmt.where();
        var in = (Condition.In) not.inner();
        assertThat(in.values()).containsExactly(5L, 6L);
    }

    @Test
    void parsesIsNullCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.users WHERE email IS NULL");
        assertThat(stmt.where()).isInstanceOf(Condition.IsNull.class);
        var isn = (Condition.IsNull) stmt.where();
        assertThat(((Expr.Column) isn.expr()).name()).isEqualTo("email");
    }

    @Test
    void parsesIsNotNullCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.users WHERE email IS NOT NULL");
        var not = (Condition.Not) stmt.where();
        assertThat(not.inner()).isInstanceOf(Condition.IsNull.class);
    }

    @Test
    void parsesLikeCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.users WHERE name LIKE 'Jo%'");
        var like = (Condition.Like) stmt.where();
        assertThat(like.pattern()).isEqualTo("Jo%");
        assertThat(((Expr.Column) like.expr()).name()).isEqualTo("name");
    }

    @Test
    void parsesNotLikeCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.users WHERE name NOT LIKE '%test%'");
        var not = (Condition.Not) stmt.where();
        assertThat(not.inner()).isInstanceOf(Condition.Like.class);
    }

    @Test
    void parsesNotCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.users WHERE NOT (active = false)");
        var not = (Condition.Not) stmt.where();
        assertThat(not.inner()).isInstanceOf(Condition.Compare.class);
    }

    @Test
    void parsesBetweenCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.orders WHERE total BETWEEN 10.0 AND 100.0");
        var between = (Condition.Between) stmt.where();
        assertThat(between.low()).isEqualTo(10.0);
        assertThat(between.high()).isEqualTo(100.0);
        assertThat(((Expr.Column) between.expr()).name()).isEqualTo("total");
    }

    @Test
    void parsesBetweenWithAndCondition() {
        var stmt = (SelectStatement) facade.parse(
            "SELECT * FROM pg.orders WHERE total BETWEEN 10 AND 100 AND active = true");
        var and = (Condition.And) stmt.where();
        assertThat(and.left()).isInstanceOf(Condition.Between.class);
        assertThat(and.right()).isInstanceOf(Condition.Compare.class);
    }
}
