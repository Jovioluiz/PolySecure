// Generated from com/polysecure/parser/SqlPoly.g4 by ANTLR 4.13.1
package com.polysecure.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SqlPolyParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SqlPolyVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(SqlPolyParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#selectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectStatement(SqlPolyParser.SelectStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#selectList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectList(SqlPolyParser.SelectListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#selectItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectItem(SqlPolyParser.SelectItemContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CrossStoreTableRef}
	 * labeled alternative in {@link SqlPolyParser#tableRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCrossStoreTableRef(SqlPolyParser.CrossStoreTableRefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocalTableRef}
	 * labeled alternative in {@link SqlPolyParser#tableRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalTableRef(SqlPolyParser.LocalTableRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#joinClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinClause(SqlPolyParser.JoinClauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenCondition}
	 * labeled alternative in {@link SqlPolyParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenCondition(SqlPolyParser.ParenConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CompareCondition}
	 * labeled alternative in {@link SqlPolyParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompareCondition(SqlPolyParser.CompareConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrCondition}
	 * labeled alternative in {@link SqlPolyParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrCondition(SqlPolyParser.OrConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndCondition}
	 * labeled alternative in {@link SqlPolyParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndCondition(SqlPolyParser.AndConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#compOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompOp(SqlPolyParser.CompOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code QualifiedColumn}
	 * labeled alternative in {@link SqlPolyParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedColumn(SqlPolyParser.QualifiedColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code QualifiedStar}
	 * labeled alternative in {@link SqlPolyParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedStar(SqlPolyParser.QualifiedStarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SimpleColumn}
	 * labeled alternative in {@link SqlPolyParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleColumn(SqlPolyParser.SimpleColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralVal}
	 * labeled alternative in {@link SqlPolyParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralVal(SqlPolyParser.LiteralValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(SqlPolyParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#insertStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertStatement(SqlPolyParser.InsertStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#storeInsertClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStoreInsertClause(SqlPolyParser.StoreInsertClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#insertSelectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertSelectStatement(SqlPolyParser.InsertSelectStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#storeTargetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStoreTargetClause(SqlPolyParser.StoreTargetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#updateStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdateStatement(SqlPolyParser.UpdateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#setClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetClause(SqlPolyParser.SetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#deleteStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteStatement(SqlPolyParser.DeleteStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#createTableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableStatement(SqlPolyParser.CreateTableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#columnDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnDef(SqlPolyParser.ColumnDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#dropTableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTableStatement(SqlPolyParser.DropTableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#tableName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableName(SqlPolyParser.TableNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#storeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStoreName(SqlPolyParser.StoreNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlPolyParser#columnName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnName(SqlPolyParser.ColumnNameContext ctx);
}