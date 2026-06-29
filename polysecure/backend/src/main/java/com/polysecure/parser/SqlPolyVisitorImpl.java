package com.polysecure.parser;

import com.polysecure.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class SqlPolyVisitorImpl extends SqlPolyBaseVisitor<Object> {

    // ── Entry point ──────────────────────────────────────────────────────────

    @Override
    public Statement visitQuery(SqlPolyParser.QueryContext ctx) {
        if (ctx.selectStatement()        != null) return visitSelectStatement(ctx.selectStatement());
        if (ctx.insertStatement()        != null) return visitInsertStatement(ctx.insertStatement());
        if (ctx.insertSelectStatement()  != null) return visitInsertSelectStatement(ctx.insertSelectStatement());
        if (ctx.updateStatement()        != null) return visitUpdateStatement(ctx.updateStatement());
        if (ctx.deleteStatement()        != null) return visitDeleteStatement(ctx.deleteStatement());
        if (ctx.createTableStatement()   != null) return visitCreateTableStatement(ctx.createTableStatement());
        if (ctx.dropTableStatement()     != null) return visitDropTableStatement(ctx.dropTableStatement());
        throw new ParseException("Unknown statement type");
    }

    // ── SELECT ───────────────────────────────────────────────────────────────

    @Override
    public SelectStatement visitSelectStatement(SqlPolyParser.SelectStatementContext ctx) {
        boolean star = ctx.selectList().STAR() != null;
        List<ColumnRef> projections = star ? List.of()
            : ctx.selectList().selectItem().stream()
                  .map(item -> (ColumnRef) visit(item))
                  .collect(Collectors.toList());
        TableRef from = (TableRef) visit(ctx.tableRef());
        List<JoinClause> joins = ctx.joinClause().stream()
            .map(j -> (JoinClause) visit(j))
            .collect(Collectors.toList());
        Condition where = ctx.condition() != null ? (Condition) visit(ctx.condition()) : null;
        return new SelectStatement(star, projections, from, joins, where);
    }

    @Override
    public ColumnRef visitSelectItem(SqlPolyParser.SelectItemContext ctx) {
        String alias = ctx.alias != null ? ctx.alias.getText() : null;
        Object exprResult = visit(ctx.expr());
        return switch (exprResult) {
            case Expr.Column col -> new ColumnRef(col.tableAlias(), col.name(),
                alias != null ? alias : col.name());
            case Expr.Star star  -> new ColumnRef(star.tableAlias(), "*", alias);
            case Expr.Literal lit -> new ColumnRef(null, String.valueOf(lit.value()), alias);
            default -> throw new ParseException("Invalid expression in SELECT: " + ctx.getText());
        };
    }

    @Override
    public TableRef visitCrossStoreTableRef(SqlPolyParser.CrossStoreTableRefContext ctx) {
        String store = ctx.store.getText();
        String table = ctx.table.getText();
        String alias = ctx.alias != null ? ctx.alias.getText() : table;
        return new TableRef(store, table, alias);
    }

    @Override
    public TableRef visitLocalTableRef(SqlPolyParser.LocalTableRefContext ctx) {
        String table = ctx.table.getText();
        String alias = ctx.alias != null ? ctx.alias.getText() : table;
        return new TableRef(null, table, alias);
    }

    @Override
    public JoinClause visitJoinClause(SqlPolyParser.JoinClauseContext ctx) {
        return new JoinClause((TableRef) visit(ctx.tableRef()), (Condition) visit(ctx.condition()));
    }

    @Override
    public Condition visitParenCondition(SqlPolyParser.ParenConditionContext ctx) {
        return (Condition) visit(ctx.condition());
    }

    @Override
    public Condition visitNotCondition(SqlPolyParser.NotConditionContext ctx) {
        return new Condition.Not((Condition) visit(ctx.condition()));
    }

    @Override
    public Condition visitAndCondition(SqlPolyParser.AndConditionContext ctx) {
        return new Condition.And((Condition) visit(ctx.left), (Condition) visit(ctx.right));
    }

    @Override
    public Condition visitOrCondition(SqlPolyParser.OrConditionContext ctx) {
        return new Condition.Or((Condition) visit(ctx.left), (Condition) visit(ctx.right));
    }

    @Override
    public Condition visitCompareCondition(SqlPolyParser.CompareConditionContext ctx) {
        return new Condition.Compare((Expr) visit(ctx.left), ctx.op.getText(), (Expr) visit(ctx.right));
    }

    @Override
    public Condition visitInCondition(SqlPolyParser.InConditionContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        List<Object> values = ctx.literal().stream().map(this::parseLiteralCtx).collect(Collectors.toList());
        return new Condition.In(expr, values);
    }

    @Override
    public Condition visitNotInCondition(SqlPolyParser.NotInConditionContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        List<Object> values = ctx.literal().stream().map(this::parseLiteralCtx).collect(Collectors.toList());
        return new Condition.Not(new Condition.In(expr, values));
    }

    @Override
    public Condition visitIsNullCondition(SqlPolyParser.IsNullConditionContext ctx) {
        return new Condition.IsNull((Expr) visit(ctx.expr()));
    }

    @Override
    public Condition visitIsNotNullCondition(SqlPolyParser.IsNotNullConditionContext ctx) {
        return new Condition.Not(new Condition.IsNull((Expr) visit(ctx.expr())));
    }

    @Override
    public Condition visitLikeCondition(SqlPolyParser.LikeConditionContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        String raw = ctx.val.getText();
        String pattern = raw.substring(1, raw.length() - 1).replace("''", "'");
        return new Condition.Like(expr, pattern);
    }

    @Override
    public Condition visitNotLikeCondition(SqlPolyParser.NotLikeConditionContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        String raw = ctx.val.getText();
        String pattern = raw.substring(1, raw.length() - 1).replace("''", "'");
        return new Condition.Not(new Condition.Like(expr, pattern));
    }

    @Override
    public Condition visitBetweenCondition(SqlPolyParser.BetweenConditionContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        Object low = parseLiteralCtx(ctx.low);
        Object high = parseLiteralCtx(ctx.high);
        return new Condition.Between(expr, low, high);
    }

    @Override
    public Expr visitQualifiedColumn(SqlPolyParser.QualifiedColumnContext ctx) {
        return new Expr.Column(ctx.qualifier.getText(), ctx.col.getText());
    }

    @Override
    public Expr visitQualifiedStar(SqlPolyParser.QualifiedStarContext ctx) {
        return new Expr.Star(ctx.qualifier.getText());
    }

    @Override
    public Expr visitSimpleColumn(SqlPolyParser.SimpleColumnContext ctx) {
        return new Expr.Column(null, ctx.name.getText());
    }

    @Override
    public Expr visitLiteralVal(SqlPolyParser.LiteralValContext ctx) {
        return new Expr.Literal(parseLiteralCtx(ctx.val));
    }

    // ── INSERT (values) ──────────────────────────────────────────────────────

    @Override
    public InsertStatement visitInsertStatement(SqlPolyParser.InsertStatementContext ctx) {
        String table = ctx.tableName().getText();
        List<StoreInsertClause> clauses = ctx.storeInsertClause().stream()
            .map(this::visitStoreInsertClause)
            .collect(Collectors.toList());
        return new InsertStatement(table, clauses);
    }

    @Override
    public StoreInsertClause visitStoreInsertClause(SqlPolyParser.StoreInsertClauseContext ctx) {
        String store = ctx.storeName().getText();
        List<String> cols = ctx.columnName().stream()
            .map(c -> c.getText())
            .collect(Collectors.toList());
        List<Object> vals = ctx.literal().stream()
            .map(this::parseLiteralCtx)
            .collect(Collectors.toList());
        return new StoreInsertClause(store, cols, vals);
    }

    // ── INSERT … SELECT ──────────────────────────────────────────────────────

    @Override
    public InsertSelectStatement visitInsertSelectStatement(SqlPolyParser.InsertSelectStatementContext ctx) {
        String table = ctx.tableName().getText();
        List<StoreTargetClause> targets = ctx.storeTargetClause().stream()
            .map(this::visitStoreTargetClause)
            .collect(Collectors.toList());
        SelectStatement source = visitSelectStatement(ctx.selectStatement());
        return new InsertSelectStatement(table, targets, source);
    }

    public StoreTargetClause visitStoreTargetClause(SqlPolyParser.StoreTargetClauseContext ctx) {
        String store = ctx.storeName().getText();
        List<String> cols = ctx.columnName().stream()
            .map(c -> c.getText())
            .collect(Collectors.toList());
        return new StoreTargetClause(store, cols);
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    public UpdateStatement visitUpdateStatement(SqlPolyParser.UpdateStatementContext ctx) {
        String table = ctx.tableName().getText();
        List<SetClause> sets = ctx.setClause().stream()
            .map(this::visitSetClause)
            .collect(Collectors.toList());
        Condition where = ctx.condition() != null ? (Condition) visit(ctx.condition()) : null;
        return new UpdateStatement(table, sets, where);
    }

    @Override
    public SetClause visitSetClause(SqlPolyParser.SetClauseContext ctx) {
        String store = ctx.store != null ? ctx.store.getText() : null;
        String col = ctx.col.getText();
        Object val = parseLiteralCtx(ctx.val);
        return new SetClause(store, col, val);
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Override
    public DeleteStatement visitDeleteStatement(SqlPolyParser.DeleteStatementContext ctx) {
        String table = ctx.tableName().getText();
        Condition where = ctx.condition() != null ? (Condition) visit(ctx.condition()) : null;
        return new DeleteStatement(table, where);
    }

    // ── CREATE TABLE ─────────────────────────────────────────────────────────

    @Override
    public CreateTableStatement visitCreateTableStatement(SqlPolyParser.CreateTableStatementContext ctx) {
        String table = ctx.tableName().getText();
        List<ColumnDefinition> cols = ctx.columnDef().stream()
            .map(this::visitColumnDef)
            .collect(Collectors.toList());
        return new CreateTableStatement(table, cols);
    }

    @Override
    public ColumnDefinition visitColumnDef(SqlPolyParser.ColumnDefContext ctx) {
        boolean pk = ctx.K_PRIMARY() != null && ctx.K_KEY() != null;
        return new ColumnDefinition(ctx.name.getText(), ctx.type.getText(), ctx.store.getText(), pk);
    }

    // ── DROP TABLE ───────────────────────────────────────────────────────────

    @Override
    public DropTableStatement visitDropTableStatement(SqlPolyParser.DropTableStatementContext ctx) {
        return new DropTableStatement(ctx.tableName().getText());
    }

    // ── Literal parsing ──────────────────────────────────────────────────────

    private Object parseLiteralCtx(SqlPolyParser.LiteralContext ctx) {
        if (ctx.STRING_LITERAL() != null) {
            String text = ctx.STRING_LITERAL().getText();
            return text.substring(1, text.length() - 1).replace("''", "'");
        }
        if (ctx.NUMBER()  != null) return Double.parseDouble(ctx.NUMBER().getText());
        if (ctx.K_TRUE()  != null) return true;
        if (ctx.K_FALSE() != null) return false;
        if (ctx.K_NULL()  != null) return null;
        throw new ParseException("Unknown literal: " + ctx.getText());
    }
}
