// Generated from com/polysecure/parser/SqlPoly.g4 by ANTLR 4.13.1
package com.polysecure.parser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class SqlPolyParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		K_SELECT=1, K_FROM=2, K_WHERE=3, K_JOIN=4, K_ON=5, K_AND=6, K_OR=7, K_AS=8, 
		K_NULL=9, K_TRUE=10, K_FALSE=11, K_INSERT=12, K_INTO=13, K_VALUES=14, 
		K_UPDATE=15, K_SET=16, K_DELETE=17, K_CREATE=18, K_DROP=19, K_TABLE=20, 
		K_POLYSTORE=21, K_STORE=22, K_PRIMARY=23, K_KEY=24, EQ=25, NEQ=26, GT=27, 
		LT=28, GTE=29, LTE=30, STAR=31, DOT=32, COMMA=33, LPAREN=34, RPAREN=35, 
		SEMICOLON=36, STRING_LITERAL=37, NUMBER=38, IDENTIFIER=39, WS=40;
	public static final int
		RULE_query = 0, RULE_selectStatement = 1, RULE_selectList = 2, RULE_selectItem = 3, 
		RULE_tableRef = 4, RULE_joinClause = 5, RULE_condition = 6, RULE_compOp = 7, 
		RULE_expr = 8, RULE_literal = 9, RULE_insertStatement = 10, RULE_storeInsertClause = 11, 
		RULE_insertSelectStatement = 12, RULE_storeTargetClause = 13, RULE_updateStatement = 14, 
		RULE_setClause = 15, RULE_deleteStatement = 16, RULE_createTableStatement = 17, 
		RULE_columnDef = 18, RULE_dropTableStatement = 19, RULE_tableName = 20, 
		RULE_storeName = 21, RULE_columnName = 22;
	private static String[] makeRuleNames() {
		return new String[] {
			"query", "selectStatement", "selectList", "selectItem", "tableRef", "joinClause", 
			"condition", "compOp", "expr", "literal", "insertStatement", "storeInsertClause", 
			"insertSelectStatement", "storeTargetClause", "updateStatement", "setClause", 
			"deleteStatement", "createTableStatement", "columnDef", "dropTableStatement", 
			"tableName", "storeName", "columnName"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, "'='", null, "'>'", "'<'", "'>='", "'<='", "'*'", "'.'", "','", 
			"'('", "')'", "';'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "K_SELECT", "K_FROM", "K_WHERE", "K_JOIN", "K_ON", "K_AND", "K_OR", 
			"K_AS", "K_NULL", "K_TRUE", "K_FALSE", "K_INSERT", "K_INTO", "K_VALUES", 
			"K_UPDATE", "K_SET", "K_DELETE", "K_CREATE", "K_DROP", "K_TABLE", "K_POLYSTORE", 
			"K_STORE", "K_PRIMARY", "K_KEY", "EQ", "NEQ", "GT", "LT", "GTE", "LTE", 
			"STAR", "DOT", "COMMA", "LPAREN", "RPAREN", "SEMICOLON", "STRING_LITERAL", 
			"NUMBER", "IDENTIFIER", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "SqlPoly.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SqlPolyParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QueryContext extends ParserRuleContext {
		public SelectStatementContext selectStatement() {
			return getRuleContext(SelectStatementContext.class,0);
		}
		public TerminalNode EOF() { return getToken(SqlPolyParser.EOF, 0); }
		public InsertStatementContext insertStatement() {
			return getRuleContext(InsertStatementContext.class,0);
		}
		public InsertSelectStatementContext insertSelectStatement() {
			return getRuleContext(InsertSelectStatementContext.class,0);
		}
		public UpdateStatementContext updateStatement() {
			return getRuleContext(UpdateStatementContext.class,0);
		}
		public DeleteStatementContext deleteStatement() {
			return getRuleContext(DeleteStatementContext.class,0);
		}
		public CreateTableStatementContext createTableStatement() {
			return getRuleContext(CreateTableStatementContext.class,0);
		}
		public DropTableStatementContext dropTableStatement() {
			return getRuleContext(DropTableStatementContext.class,0);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_query);
		try {
			setState(67);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(46);
				selectStatement();
				setState(47);
				match(EOF);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(49);
				insertStatement();
				setState(50);
				match(EOF);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(52);
				insertSelectStatement();
				setState(53);
				match(EOF);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(55);
				updateStatement();
				setState(56);
				match(EOF);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(58);
				deleteStatement();
				setState(59);
				match(EOF);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(61);
				createTableStatement();
				setState(62);
				match(EOF);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(64);
				dropTableStatement();
				setState(65);
				match(EOF);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectStatementContext extends ParserRuleContext {
		public TerminalNode K_SELECT() { return getToken(SqlPolyParser.K_SELECT, 0); }
		public SelectListContext selectList() {
			return getRuleContext(SelectListContext.class,0);
		}
		public TerminalNode K_FROM() { return getToken(SqlPolyParser.K_FROM, 0); }
		public TableRefContext tableRef() {
			return getRuleContext(TableRefContext.class,0);
		}
		public List<JoinClauseContext> joinClause() {
			return getRuleContexts(JoinClauseContext.class);
		}
		public JoinClauseContext joinClause(int i) {
			return getRuleContext(JoinClauseContext.class,i);
		}
		public TerminalNode K_WHERE() { return getToken(SqlPolyParser.K_WHERE, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(SqlPolyParser.SEMICOLON, 0); }
		public SelectStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitSelectStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectStatementContext selectStatement() throws RecognitionException {
		SelectStatementContext _localctx = new SelectStatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_selectStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(K_SELECT);
			setState(70);
			selectList();
			setState(71);
			match(K_FROM);
			setState(72);
			tableRef();
			setState(76);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==K_JOIN) {
				{
				{
				setState(73);
				joinClause();
				}
				}
				setState(78);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(81);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==K_WHERE) {
				{
				setState(79);
				match(K_WHERE);
				setState(80);
				condition(0);
				}
			}

			setState(84);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(83);
				match(SEMICOLON);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectListContext extends ParserRuleContext {
		public TerminalNode STAR() { return getToken(SqlPolyParser.STAR, 0); }
		public List<SelectItemContext> selectItem() {
			return getRuleContexts(SelectItemContext.class);
		}
		public SelectItemContext selectItem(int i) {
			return getRuleContext(SelectItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SqlPolyParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlPolyParser.COMMA, i);
		}
		public SelectListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitSelectList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectListContext selectList() throws RecognitionException {
		SelectListContext _localctx = new SelectListContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_selectList);
		int _la;
		try {
			setState(95);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(86);
				match(STAR);
				}
				break;
			case K_NULL:
			case K_TRUE:
			case K_FALSE:
			case STRING_LITERAL:
			case NUMBER:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(87);
				selectItem();
				setState(92);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(88);
					match(COMMA);
					setState(89);
					selectItem();
					}
					}
					setState(94);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectItemContext extends ParserRuleContext {
		public Token alias;
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(SqlPolyParser.IDENTIFIER, 0); }
		public TerminalNode K_AS() { return getToken(SqlPolyParser.K_AS, 0); }
		public SelectItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectItem; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitSelectItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectItemContext selectItem() throws RecognitionException {
		SelectItemContext _localctx = new SelectItemContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_selectItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			expr();
			setState(102);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==K_AS || _la==IDENTIFIER) {
				{
				setState(99);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==K_AS) {
					{
					setState(98);
					match(K_AS);
					}
				}

				setState(101);
				((SelectItemContext)_localctx).alias = match(IDENTIFIER);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TableRefContext extends ParserRuleContext {
		public TableRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableRef; }
	 
		public TableRefContext() { }
		public void copyFrom(TableRefContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CrossStoreTableRefContext extends TableRefContext {
		public Token store;
		public Token table;
		public Token alias;
		public TerminalNode DOT() { return getToken(SqlPolyParser.DOT, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(SqlPolyParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(SqlPolyParser.IDENTIFIER, i);
		}
		public TerminalNode K_AS() { return getToken(SqlPolyParser.K_AS, 0); }
		public CrossStoreTableRefContext(TableRefContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitCrossStoreTableRef(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LocalTableRefContext extends TableRefContext {
		public Token table;
		public Token alias;
		public List<TerminalNode> IDENTIFIER() { return getTokens(SqlPolyParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(SqlPolyParser.IDENTIFIER, i);
		}
		public TerminalNode K_AS() { return getToken(SqlPolyParser.K_AS, 0); }
		public LocalTableRefContext(TableRefContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitLocalTableRef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableRefContext tableRef() throws RecognitionException {
		TableRefContext _localctx = new TableRefContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_tableRef);
		int _la;
		try {
			setState(120);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				_localctx = new CrossStoreTableRefContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(104);
				((CrossStoreTableRefContext)_localctx).store = match(IDENTIFIER);
				setState(105);
				match(DOT);
				setState(106);
				((CrossStoreTableRefContext)_localctx).table = match(IDENTIFIER);
				setState(111);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==K_AS || _la==IDENTIFIER) {
					{
					setState(108);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==K_AS) {
						{
						setState(107);
						match(K_AS);
						}
					}

					setState(110);
					((CrossStoreTableRefContext)_localctx).alias = match(IDENTIFIER);
					}
				}

				}
				break;
			case 2:
				_localctx = new LocalTableRefContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				((LocalTableRefContext)_localctx).table = match(IDENTIFIER);
				setState(118);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==K_AS || _la==IDENTIFIER) {
					{
					setState(115);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==K_AS) {
						{
						setState(114);
						match(K_AS);
						}
					}

					setState(117);
					((LocalTableRefContext)_localctx).alias = match(IDENTIFIER);
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class JoinClauseContext extends ParserRuleContext {
		public TerminalNode K_JOIN() { return getToken(SqlPolyParser.K_JOIN, 0); }
		public TableRefContext tableRef() {
			return getRuleContext(TableRefContext.class,0);
		}
		public TerminalNode K_ON() { return getToken(SqlPolyParser.K_ON, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public JoinClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitJoinClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinClauseContext joinClause() throws RecognitionException {
		JoinClauseContext _localctx = new JoinClauseContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_joinClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(K_JOIN);
			setState(123);
			tableRef();
			setState(124);
			match(K_ON);
			setState(125);
			condition(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConditionContext extends ParserRuleContext {
		public ConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_condition; }
	 
		public ConditionContext() { }
		public void copyFrom(ConditionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenConditionContext extends ConditionContext {
		public TerminalNode LPAREN() { return getToken(SqlPolyParser.LPAREN, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(SqlPolyParser.RPAREN, 0); }
		public ParenConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitParenCondition(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CompareConditionContext extends ConditionContext {
		public ExprContext left;
		public CompOpContext op;
		public ExprContext right;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public CompOpContext compOp() {
			return getRuleContext(CompOpContext.class,0);
		}
		public CompareConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitCompareCondition(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OrConditionContext extends ConditionContext {
		public ConditionContext left;
		public ConditionContext right;
		public TerminalNode K_OR() { return getToken(SqlPolyParser.K_OR, 0); }
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public OrConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitOrCondition(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AndConditionContext extends ConditionContext {
		public ConditionContext left;
		public ConditionContext right;
		public TerminalNode K_AND() { return getToken(SqlPolyParser.K_AND, 0); }
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public AndConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitAndCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionContext condition() throws RecognitionException {
		return condition(0);
	}

	private ConditionContext condition(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ConditionContext _localctx = new ConditionContext(_ctx, _parentState);
		ConditionContext _prevctx = _localctx;
		int _startState = 12;
		enterRecursionRule(_localctx, 12, RULE_condition, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(136);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				{
				_localctx = new ParenConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(128);
				match(LPAREN);
				setState(129);
				condition(0);
				setState(130);
				match(RPAREN);
				}
				break;
			case K_NULL:
			case K_TRUE:
			case K_FALSE:
			case STRING_LITERAL:
			case NUMBER:
			case IDENTIFIER:
				{
				_localctx = new CompareConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(132);
				((CompareConditionContext)_localctx).left = expr();
				setState(133);
				((CompareConditionContext)_localctx).op = compOp();
				setState(134);
				((CompareConditionContext)_localctx).right = expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(146);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(144);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
					case 1:
						{
						_localctx = new OrConditionContext(new ConditionContext(_parentctx, _parentState));
						((OrConditionContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_condition);
						setState(138);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(139);
						match(K_OR);
						setState(140);
						((OrConditionContext)_localctx).right = condition(4);
						}
						break;
					case 2:
						{
						_localctx = new AndConditionContext(new ConditionContext(_parentctx, _parentState));
						((AndConditionContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_condition);
						setState(141);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(142);
						match(K_AND);
						setState(143);
						((AndConditionContext)_localctx).right = condition(3);
						}
						break;
					}
					} 
				}
				setState(148);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CompOpContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(SqlPolyParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(SqlPolyParser.NEQ, 0); }
		public TerminalNode GT() { return getToken(SqlPolyParser.GT, 0); }
		public TerminalNode LT() { return getToken(SqlPolyParser.LT, 0); }
		public TerminalNode GTE() { return getToken(SqlPolyParser.GTE, 0); }
		public TerminalNode LTE() { return getToken(SqlPolyParser.LTE, 0); }
		public CompOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compOp; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitCompOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompOpContext compOp() throws RecognitionException {
		CompOpContext _localctx = new CompOpContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_compOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2113929216L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SimpleColumnContext extends ExprContext {
		public Token name;
		public TerminalNode IDENTIFIER() { return getToken(SqlPolyParser.IDENTIFIER, 0); }
		public SimpleColumnContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitSimpleColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedStarContext extends ExprContext {
		public Token qualifier;
		public TerminalNode DOT() { return getToken(SqlPolyParser.DOT, 0); }
		public TerminalNode STAR() { return getToken(SqlPolyParser.STAR, 0); }
		public TerminalNode IDENTIFIER() { return getToken(SqlPolyParser.IDENTIFIER, 0); }
		public QualifiedStarContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitQualifiedStar(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LiteralValContext extends ExprContext {
		public LiteralContext val;
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LiteralValContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitLiteralVal(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedColumnContext extends ExprContext {
		public Token qualifier;
		public Token col;
		public TerminalNode DOT() { return getToken(SqlPolyParser.DOT, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(SqlPolyParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(SqlPolyParser.IDENTIFIER, i);
		}
		public QualifiedColumnContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitQualifiedColumn(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_expr);
		try {
			setState(159);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				_localctx = new QualifiedColumnContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(151);
				((QualifiedColumnContext)_localctx).qualifier = match(IDENTIFIER);
				setState(152);
				match(DOT);
				setState(153);
				((QualifiedColumnContext)_localctx).col = match(IDENTIFIER);
				}
				break;
			case 2:
				_localctx = new QualifiedStarContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(154);
				((QualifiedStarContext)_localctx).qualifier = match(IDENTIFIER);
				setState(155);
				match(DOT);
				setState(156);
				match(STAR);
				}
				break;
			case 3:
				_localctx = new SimpleColumnContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(157);
				((SimpleColumnContext)_localctx).name = match(IDENTIFIER);
				}
				break;
			case 4:
				_localctx = new LiteralValContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(158);
				((LiteralValContext)_localctx).val = literal();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL() { return getToken(SqlPolyParser.STRING_LITERAL, 0); }
		public TerminalNode NUMBER() { return getToken(SqlPolyParser.NUMBER, 0); }
		public TerminalNode K_TRUE() { return getToken(SqlPolyParser.K_TRUE, 0); }
		public TerminalNode K_FALSE() { return getToken(SqlPolyParser.K_FALSE, 0); }
		public TerminalNode K_NULL() { return getToken(SqlPolyParser.K_NULL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(161);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 412316864000L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertStatementContext extends ParserRuleContext {
		public TerminalNode K_INSERT() { return getToken(SqlPolyParser.K_INSERT, 0); }
		public TerminalNode K_INTO() { return getToken(SqlPolyParser.K_INTO, 0); }
		public TerminalNode K_POLYSTORE() { return getToken(SqlPolyParser.K_POLYSTORE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public List<StoreInsertClauseContext> storeInsertClause() {
			return getRuleContexts(StoreInsertClauseContext.class);
		}
		public StoreInsertClauseContext storeInsertClause(int i) {
			return getRuleContext(StoreInsertClauseContext.class,i);
		}
		public InsertStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitInsertStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertStatementContext insertStatement() throws RecognitionException {
		InsertStatementContext _localctx = new InsertStatementContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_insertStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(163);
			match(K_INSERT);
			setState(164);
			match(K_INTO);
			setState(165);
			match(K_POLYSTORE);
			setState(166);
			tableName();
			setState(168); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(167);
				storeInsertClause();
				}
				}
				setState(170); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==IDENTIFIER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StoreInsertClauseContext extends ParserRuleContext {
		public StoreNameContext storeName() {
			return getRuleContext(StoreNameContext.class,0);
		}
		public List<TerminalNode> LPAREN() { return getTokens(SqlPolyParser.LPAREN); }
		public TerminalNode LPAREN(int i) {
			return getToken(SqlPolyParser.LPAREN, i);
		}
		public List<ColumnNameContext> columnName() {
			return getRuleContexts(ColumnNameContext.class);
		}
		public ColumnNameContext columnName(int i) {
			return getRuleContext(ColumnNameContext.class,i);
		}
		public List<TerminalNode> RPAREN() { return getTokens(SqlPolyParser.RPAREN); }
		public TerminalNode RPAREN(int i) {
			return getToken(SqlPolyParser.RPAREN, i);
		}
		public TerminalNode K_VALUES() { return getToken(SqlPolyParser.K_VALUES, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SqlPolyParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlPolyParser.COMMA, i);
		}
		public StoreInsertClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storeInsertClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitStoreInsertClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StoreInsertClauseContext storeInsertClause() throws RecognitionException {
		StoreInsertClauseContext _localctx = new StoreInsertClauseContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_storeInsertClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(172);
			storeName();
			setState(173);
			match(LPAREN);
			setState(174);
			columnName();
			setState(179);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(175);
				match(COMMA);
				setState(176);
				columnName();
				}
				}
				setState(181);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(182);
			match(RPAREN);
			setState(183);
			match(K_VALUES);
			setState(184);
			match(LPAREN);
			setState(185);
			literal();
			setState(190);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(186);
				match(COMMA);
				setState(187);
				literal();
				}
				}
				setState(192);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(193);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertSelectStatementContext extends ParserRuleContext {
		public TerminalNode K_INSERT() { return getToken(SqlPolyParser.K_INSERT, 0); }
		public TerminalNode K_INTO() { return getToken(SqlPolyParser.K_INTO, 0); }
		public TerminalNode K_POLYSTORE() { return getToken(SqlPolyParser.K_POLYSTORE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public SelectStatementContext selectStatement() {
			return getRuleContext(SelectStatementContext.class,0);
		}
		public List<StoreTargetClauseContext> storeTargetClause() {
			return getRuleContexts(StoreTargetClauseContext.class);
		}
		public StoreTargetClauseContext storeTargetClause(int i) {
			return getRuleContext(StoreTargetClauseContext.class,i);
		}
		public InsertSelectStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertSelectStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitInsertSelectStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertSelectStatementContext insertSelectStatement() throws RecognitionException {
		InsertSelectStatementContext _localctx = new InsertSelectStatementContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_insertSelectStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(195);
			match(K_INSERT);
			setState(196);
			match(K_INTO);
			setState(197);
			match(K_POLYSTORE);
			setState(198);
			tableName();
			setState(200); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(199);
				storeTargetClause();
				}
				}
				setState(202); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==IDENTIFIER );
			setState(204);
			selectStatement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StoreTargetClauseContext extends ParserRuleContext {
		public StoreNameContext storeName() {
			return getRuleContext(StoreNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(SqlPolyParser.LPAREN, 0); }
		public List<ColumnNameContext> columnName() {
			return getRuleContexts(ColumnNameContext.class);
		}
		public ColumnNameContext columnName(int i) {
			return getRuleContext(ColumnNameContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(SqlPolyParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SqlPolyParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlPolyParser.COMMA, i);
		}
		public StoreTargetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storeTargetClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitStoreTargetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StoreTargetClauseContext storeTargetClause() throws RecognitionException {
		StoreTargetClauseContext _localctx = new StoreTargetClauseContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_storeTargetClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			storeName();
			setState(207);
			match(LPAREN);
			setState(208);
			columnName();
			setState(213);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(209);
				match(COMMA);
				setState(210);
				columnName();
				}
				}
				setState(215);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(216);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UpdateStatementContext extends ParserRuleContext {
		public TerminalNode K_UPDATE() { return getToken(SqlPolyParser.K_UPDATE, 0); }
		public TerminalNode K_POLYSTORE() { return getToken(SqlPolyParser.K_POLYSTORE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public TerminalNode K_SET() { return getToken(SqlPolyParser.K_SET, 0); }
		public List<SetClauseContext> setClause() {
			return getRuleContexts(SetClauseContext.class);
		}
		public SetClauseContext setClause(int i) {
			return getRuleContext(SetClauseContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SqlPolyParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlPolyParser.COMMA, i);
		}
		public TerminalNode K_WHERE() { return getToken(SqlPolyParser.K_WHERE, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public UpdateStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_updateStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitUpdateStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateStatementContext updateStatement() throws RecognitionException {
		UpdateStatementContext _localctx = new UpdateStatementContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_updateStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(218);
			match(K_UPDATE);
			setState(219);
			match(K_POLYSTORE);
			setState(220);
			tableName();
			setState(221);
			match(K_SET);
			setState(222);
			setClause();
			setState(227);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(223);
				match(COMMA);
				setState(224);
				setClause();
				}
				}
				setState(229);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(232);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==K_WHERE) {
				{
				setState(230);
				match(K_WHERE);
				setState(231);
				condition(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SetClauseContext extends ParserRuleContext {
		public Token store;
		public Token col;
		public LiteralContext val;
		public TerminalNode EQ() { return getToken(SqlPolyParser.EQ, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(SqlPolyParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(SqlPolyParser.IDENTIFIER, i);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode DOT() { return getToken(SqlPolyParser.DOT, 0); }
		public SetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitSetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetClauseContext setClause() throws RecognitionException {
		SetClauseContext _localctx = new SetClauseContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_setClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(236);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(234);
				((SetClauseContext)_localctx).store = match(IDENTIFIER);
				setState(235);
				match(DOT);
				}
				break;
			}
			setState(238);
			((SetClauseContext)_localctx).col = match(IDENTIFIER);
			setState(239);
			match(EQ);
			setState(240);
			((SetClauseContext)_localctx).val = literal();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeleteStatementContext extends ParserRuleContext {
		public TerminalNode K_DELETE() { return getToken(SqlPolyParser.K_DELETE, 0); }
		public TerminalNode K_FROM() { return getToken(SqlPolyParser.K_FROM, 0); }
		public TerminalNode K_POLYSTORE() { return getToken(SqlPolyParser.K_POLYSTORE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public TerminalNode K_WHERE() { return getToken(SqlPolyParser.K_WHERE, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public DeleteStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deleteStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitDeleteStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteStatementContext deleteStatement() throws RecognitionException {
		DeleteStatementContext _localctx = new DeleteStatementContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_deleteStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(242);
			match(K_DELETE);
			setState(243);
			match(K_FROM);
			setState(244);
			match(K_POLYSTORE);
			setState(245);
			tableName();
			setState(248);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==K_WHERE) {
				{
				setState(246);
				match(K_WHERE);
				setState(247);
				condition(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateTableStatementContext extends ParserRuleContext {
		public TerminalNode K_CREATE() { return getToken(SqlPolyParser.K_CREATE, 0); }
		public TerminalNode K_POLYSTORE() { return getToken(SqlPolyParser.K_POLYSTORE, 0); }
		public TerminalNode K_TABLE() { return getToken(SqlPolyParser.K_TABLE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(SqlPolyParser.LPAREN, 0); }
		public List<ColumnDefContext> columnDef() {
			return getRuleContexts(ColumnDefContext.class);
		}
		public ColumnDefContext columnDef(int i) {
			return getRuleContext(ColumnDefContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(SqlPolyParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SqlPolyParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlPolyParser.COMMA, i);
		}
		public CreateTableStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitCreateTableStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableStatementContext createTableStatement() throws RecognitionException {
		CreateTableStatementContext _localctx = new CreateTableStatementContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_createTableStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			match(K_CREATE);
			setState(251);
			match(K_POLYSTORE);
			setState(252);
			match(K_TABLE);
			setState(253);
			tableName();
			setState(254);
			match(LPAREN);
			setState(255);
			columnDef();
			setState(260);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(256);
				match(COMMA);
				setState(257);
				columnDef();
				}
				}
				setState(262);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(263);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ColumnDefContext extends ParserRuleContext {
		public Token name;
		public Token type;
		public Token store;
		public TerminalNode K_STORE() { return getToken(SqlPolyParser.K_STORE, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(SqlPolyParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(SqlPolyParser.IDENTIFIER, i);
		}
		public TerminalNode K_PRIMARY() { return getToken(SqlPolyParser.K_PRIMARY, 0); }
		public TerminalNode K_KEY() { return getToken(SqlPolyParser.K_KEY, 0); }
		public ColumnDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnDef; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitColumnDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColumnDefContext columnDef() throws RecognitionException {
		ColumnDefContext _localctx = new ColumnDefContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_columnDef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			((ColumnDefContext)_localctx).name = match(IDENTIFIER);
			setState(266);
			((ColumnDefContext)_localctx).type = match(IDENTIFIER);
			setState(267);
			match(K_STORE);
			setState(268);
			((ColumnDefContext)_localctx).store = match(IDENTIFIER);
			setState(271);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==K_PRIMARY) {
				{
				setState(269);
				match(K_PRIMARY);
				setState(270);
				match(K_KEY);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropTableStatementContext extends ParserRuleContext {
		public TerminalNode K_DROP() { return getToken(SqlPolyParser.K_DROP, 0); }
		public TerminalNode K_POLYSTORE() { return getToken(SqlPolyParser.K_POLYSTORE, 0); }
		public TerminalNode K_TABLE() { return getToken(SqlPolyParser.K_TABLE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public DropTableStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropTableStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitDropTableStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DropTableStatementContext dropTableStatement() throws RecognitionException {
		DropTableStatementContext _localctx = new DropTableStatementContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_dropTableStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			match(K_DROP);
			setState(274);
			match(K_POLYSTORE);
			setState(275);
			match(K_TABLE);
			setState(276);
			tableName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TableNameContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(SqlPolyParser.IDENTIFIER, 0); }
		public TableNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableName; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitTableName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableNameContext tableName() throws RecognitionException {
		TableNameContext _localctx = new TableNameContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_tableName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StoreNameContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(SqlPolyParser.IDENTIFIER, 0); }
		public StoreNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storeName; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitStoreName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StoreNameContext storeName() throws RecognitionException {
		StoreNameContext _localctx = new StoreNameContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_storeName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ColumnNameContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(SqlPolyParser.IDENTIFIER, 0); }
		public ColumnNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnName; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlPolyVisitor ) return ((SqlPolyVisitor<? extends T>)visitor).visitColumnName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColumnNameContext columnName() throws RecognitionException {
		ColumnNameContext _localctx = new ColumnNameContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_columnName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 6:
			return condition_sempred((ConditionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean condition_sempred(ConditionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001(\u011d\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000"+
		"D\b\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0005\u0001K\b\u0001\n\u0001\f\u0001N\t\u0001\u0001\u0001\u0001\u0001"+
		"\u0003\u0001R\b\u0001\u0001\u0001\u0003\u0001U\b\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002[\b\u0002\n\u0002\f\u0002^\t"+
		"\u0002\u0003\u0002`\b\u0002\u0001\u0003\u0001\u0003\u0003\u0003d\b\u0003"+
		"\u0001\u0003\u0003\u0003g\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004m\b\u0004\u0001\u0004\u0003\u0004p\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004t\b\u0004\u0001\u0004\u0003\u0004w\b\u0004"+
		"\u0003\u0004y\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u0089\b\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0005\u0006\u0091\b\u0006\n\u0006\f\u0006\u0094\t\u0006\u0001\u0007\u0001"+
		"\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b"+
		"\u0003\b\u00a0\b\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0004\n\u00a9\b\n\u000b\n\f\n\u00aa\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0005\u000b\u00b2\b\u000b\n\u000b\f\u000b\u00b5"+
		"\t\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0005\u000b\u00bd\b\u000b\n\u000b\f\u000b\u00c0\t\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0004\f\u00c9\b\f"+
		"\u000b\f\f\f\u00ca\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0005\r\u00d4\b\r\n\r\f\r\u00d7\t\r\u0001\r\u0001\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005"+
		"\u000e\u00e2\b\u000e\n\u000e\f\u000e\u00e5\t\u000e\u0001\u000e\u0001\u000e"+
		"\u0003\u000e\u00e9\b\u000e\u0001\u000f\u0001\u000f\u0003\u000f\u00ed\b"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u00f9"+
		"\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u0103\b\u0011\n\u0011\f\u0011"+
		"\u0106\t\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0003\u0012\u0110\b\u0012\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0000\u0001"+
		"\f\u0017\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,\u0000\u0002\u0001\u0000\u0019\u001e\u0002\u0000"+
		"\t\u000b%&\u0128\u0000C\u0001\u0000\u0000\u0000\u0002E\u0001\u0000\u0000"+
		"\u0000\u0004_\u0001\u0000\u0000\u0000\u0006a\u0001\u0000\u0000\u0000\b"+
		"x\u0001\u0000\u0000\u0000\nz\u0001\u0000\u0000\u0000\f\u0088\u0001\u0000"+
		"\u0000\u0000\u000e\u0095\u0001\u0000\u0000\u0000\u0010\u009f\u0001\u0000"+
		"\u0000\u0000\u0012\u00a1\u0001\u0000\u0000\u0000\u0014\u00a3\u0001\u0000"+
		"\u0000\u0000\u0016\u00ac\u0001\u0000\u0000\u0000\u0018\u00c3\u0001\u0000"+
		"\u0000\u0000\u001a\u00ce\u0001\u0000\u0000\u0000\u001c\u00da\u0001\u0000"+
		"\u0000\u0000\u001e\u00ec\u0001\u0000\u0000\u0000 \u00f2\u0001\u0000\u0000"+
		"\u0000\"\u00fa\u0001\u0000\u0000\u0000$\u0109\u0001\u0000\u0000\u0000"+
		"&\u0111\u0001\u0000\u0000\u0000(\u0116\u0001\u0000\u0000\u0000*\u0118"+
		"\u0001\u0000\u0000\u0000,\u011a\u0001\u0000\u0000\u0000./\u0003\u0002"+
		"\u0001\u0000/0\u0005\u0000\u0000\u00010D\u0001\u0000\u0000\u000012\u0003"+
		"\u0014\n\u000023\u0005\u0000\u0000\u00013D\u0001\u0000\u0000\u000045\u0003"+
		"\u0018\f\u000056\u0005\u0000\u0000\u00016D\u0001\u0000\u0000\u000078\u0003"+
		"\u001c\u000e\u000089\u0005\u0000\u0000\u00019D\u0001\u0000\u0000\u0000"+
		":;\u0003 \u0010\u0000;<\u0005\u0000\u0000\u0001<D\u0001\u0000\u0000\u0000"+
		"=>\u0003\"\u0011\u0000>?\u0005\u0000\u0000\u0001?D\u0001\u0000\u0000\u0000"+
		"@A\u0003&\u0013\u0000AB\u0005\u0000\u0000\u0001BD\u0001\u0000\u0000\u0000"+
		"C.\u0001\u0000\u0000\u0000C1\u0001\u0000\u0000\u0000C4\u0001\u0000\u0000"+
		"\u0000C7\u0001\u0000\u0000\u0000C:\u0001\u0000\u0000\u0000C=\u0001\u0000"+
		"\u0000\u0000C@\u0001\u0000\u0000\u0000D\u0001\u0001\u0000\u0000\u0000"+
		"EF\u0005\u0001\u0000\u0000FG\u0003\u0004\u0002\u0000GH\u0005\u0002\u0000"+
		"\u0000HL\u0003\b\u0004\u0000IK\u0003\n\u0005\u0000JI\u0001\u0000\u0000"+
		"\u0000KN\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000LM\u0001\u0000"+
		"\u0000\u0000MQ\u0001\u0000\u0000\u0000NL\u0001\u0000\u0000\u0000OP\u0005"+
		"\u0003\u0000\u0000PR\u0003\f\u0006\u0000QO\u0001\u0000\u0000\u0000QR\u0001"+
		"\u0000\u0000\u0000RT\u0001\u0000\u0000\u0000SU\u0005$\u0000\u0000TS\u0001"+
		"\u0000\u0000\u0000TU\u0001\u0000\u0000\u0000U\u0003\u0001\u0000\u0000"+
		"\u0000V`\u0005\u001f\u0000\u0000W\\\u0003\u0006\u0003\u0000XY\u0005!\u0000"+
		"\u0000Y[\u0003\u0006\u0003\u0000ZX\u0001\u0000\u0000\u0000[^\u0001\u0000"+
		"\u0000\u0000\\Z\u0001\u0000\u0000\u0000\\]\u0001\u0000\u0000\u0000]`\u0001"+
		"\u0000\u0000\u0000^\\\u0001\u0000\u0000\u0000_V\u0001\u0000\u0000\u0000"+
		"_W\u0001\u0000\u0000\u0000`\u0005\u0001\u0000\u0000\u0000af\u0003\u0010"+
		"\b\u0000bd\u0005\b\u0000\u0000cb\u0001\u0000\u0000\u0000cd\u0001\u0000"+
		"\u0000\u0000de\u0001\u0000\u0000\u0000eg\u0005\'\u0000\u0000fc\u0001\u0000"+
		"\u0000\u0000fg\u0001\u0000\u0000\u0000g\u0007\u0001\u0000\u0000\u0000"+
		"hi\u0005\'\u0000\u0000ij\u0005 \u0000\u0000jo\u0005\'\u0000\u0000km\u0005"+
		"\b\u0000\u0000lk\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000mn\u0001"+
		"\u0000\u0000\u0000np\u0005\'\u0000\u0000ol\u0001\u0000\u0000\u0000op\u0001"+
		"\u0000\u0000\u0000py\u0001\u0000\u0000\u0000qv\u0005\'\u0000\u0000rt\u0005"+
		"\b\u0000\u0000sr\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000tu\u0001"+
		"\u0000\u0000\u0000uw\u0005\'\u0000\u0000vs\u0001\u0000\u0000\u0000vw\u0001"+
		"\u0000\u0000\u0000wy\u0001\u0000\u0000\u0000xh\u0001\u0000\u0000\u0000"+
		"xq\u0001\u0000\u0000\u0000y\t\u0001\u0000\u0000\u0000z{\u0005\u0004\u0000"+
		"\u0000{|\u0003\b\u0004\u0000|}\u0005\u0005\u0000\u0000}~\u0003\f\u0006"+
		"\u0000~\u000b\u0001\u0000\u0000\u0000\u007f\u0080\u0006\u0006\uffff\uffff"+
		"\u0000\u0080\u0081\u0005\"\u0000\u0000\u0081\u0082\u0003\f\u0006\u0000"+
		"\u0082\u0083\u0005#\u0000\u0000\u0083\u0089\u0001\u0000\u0000\u0000\u0084"+
		"\u0085\u0003\u0010\b\u0000\u0085\u0086\u0003\u000e\u0007\u0000\u0086\u0087"+
		"\u0003\u0010\b\u0000\u0087\u0089\u0001\u0000\u0000\u0000\u0088\u007f\u0001"+
		"\u0000\u0000\u0000\u0088\u0084\u0001\u0000\u0000\u0000\u0089\u0092\u0001"+
		"\u0000\u0000\u0000\u008a\u008b\n\u0003\u0000\u0000\u008b\u008c\u0005\u0007"+
		"\u0000\u0000\u008c\u0091\u0003\f\u0006\u0004\u008d\u008e\n\u0002\u0000"+
		"\u0000\u008e\u008f\u0005\u0006\u0000\u0000\u008f\u0091\u0003\f\u0006\u0003"+
		"\u0090\u008a\u0001\u0000\u0000\u0000\u0090\u008d\u0001\u0000\u0000\u0000"+
		"\u0091\u0094\u0001\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000"+
		"\u0092\u0093\u0001\u0000\u0000\u0000\u0093\r\u0001\u0000\u0000\u0000\u0094"+
		"\u0092\u0001\u0000\u0000\u0000\u0095\u0096\u0007\u0000\u0000\u0000\u0096"+
		"\u000f\u0001\u0000\u0000\u0000\u0097\u0098\u0005\'\u0000\u0000\u0098\u0099"+
		"\u0005 \u0000\u0000\u0099\u00a0\u0005\'\u0000\u0000\u009a\u009b\u0005"+
		"\'\u0000\u0000\u009b\u009c\u0005 \u0000\u0000\u009c\u00a0\u0005\u001f"+
		"\u0000\u0000\u009d\u00a0\u0005\'\u0000\u0000\u009e\u00a0\u0003\u0012\t"+
		"\u0000\u009f\u0097\u0001\u0000\u0000\u0000\u009f\u009a\u0001\u0000\u0000"+
		"\u0000\u009f\u009d\u0001\u0000\u0000\u0000\u009f\u009e\u0001\u0000\u0000"+
		"\u0000\u00a0\u0011\u0001\u0000\u0000\u0000\u00a1\u00a2\u0007\u0001\u0000"+
		"\u0000\u00a2\u0013\u0001\u0000\u0000\u0000\u00a3\u00a4\u0005\f\u0000\u0000"+
		"\u00a4\u00a5\u0005\r\u0000\u0000\u00a5\u00a6\u0005\u0015\u0000\u0000\u00a6"+
		"\u00a8\u0003(\u0014\u0000\u00a7\u00a9\u0003\u0016\u000b\u0000\u00a8\u00a7"+
		"\u0001\u0000\u0000\u0000\u00a9\u00aa\u0001\u0000\u0000\u0000\u00aa\u00a8"+
		"\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000\u0000\u00ab\u0015"+
		"\u0001\u0000\u0000\u0000\u00ac\u00ad\u0003*\u0015\u0000\u00ad\u00ae\u0005"+
		"\"\u0000\u0000\u00ae\u00b3\u0003,\u0016\u0000\u00af\u00b0\u0005!\u0000"+
		"\u0000\u00b0\u00b2\u0003,\u0016\u0000\u00b1\u00af\u0001\u0000\u0000\u0000"+
		"\u00b2\u00b5\u0001\u0000\u0000\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000"+
		"\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b6\u0001\u0000\u0000\u0000"+
		"\u00b5\u00b3\u0001\u0000\u0000\u0000\u00b6\u00b7\u0005#\u0000\u0000\u00b7"+
		"\u00b8\u0005\u000e\u0000\u0000\u00b8\u00b9\u0005\"\u0000\u0000\u00b9\u00be"+
		"\u0003\u0012\t\u0000\u00ba\u00bb\u0005!\u0000\u0000\u00bb\u00bd\u0003"+
		"\u0012\t\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000\u00bd\u00c0\u0001\u0000"+
		"\u0000\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00be\u00bf\u0001\u0000"+
		"\u0000\u0000\u00bf\u00c1\u0001\u0000\u0000\u0000\u00c0\u00be\u0001\u0000"+
		"\u0000\u0000\u00c1\u00c2\u0005#\u0000\u0000\u00c2\u0017\u0001\u0000\u0000"+
		"\u0000\u00c3\u00c4\u0005\f\u0000\u0000\u00c4\u00c5\u0005\r\u0000\u0000"+
		"\u00c5\u00c6\u0005\u0015\u0000\u0000\u00c6\u00c8\u0003(\u0014\u0000\u00c7"+
		"\u00c9\u0003\u001a\r\u0000\u00c8\u00c7\u0001\u0000\u0000\u0000\u00c9\u00ca"+
		"\u0001\u0000\u0000\u0000\u00ca\u00c8\u0001\u0000\u0000\u0000\u00ca\u00cb"+
		"\u0001\u0000\u0000\u0000\u00cb\u00cc\u0001\u0000\u0000\u0000\u00cc\u00cd"+
		"\u0003\u0002\u0001\u0000\u00cd\u0019\u0001\u0000\u0000\u0000\u00ce\u00cf"+
		"\u0003*\u0015\u0000\u00cf\u00d0\u0005\"\u0000\u0000\u00d0\u00d5\u0003"+
		",\u0016\u0000\u00d1\u00d2\u0005!\u0000\u0000\u00d2\u00d4\u0003,\u0016"+
		"\u0000\u00d3\u00d1\u0001\u0000\u0000\u0000\u00d4\u00d7\u0001\u0000\u0000"+
		"\u0000\u00d5\u00d3\u0001\u0000\u0000\u0000\u00d5\u00d6\u0001\u0000\u0000"+
		"\u0000\u00d6\u00d8\u0001\u0000\u0000\u0000\u00d7\u00d5\u0001\u0000\u0000"+
		"\u0000\u00d8\u00d9\u0005#\u0000\u0000\u00d9\u001b\u0001\u0000\u0000\u0000"+
		"\u00da\u00db\u0005\u000f\u0000\u0000\u00db\u00dc\u0005\u0015\u0000\u0000"+
		"\u00dc\u00dd\u0003(\u0014\u0000\u00dd\u00de\u0005\u0010\u0000\u0000\u00de"+
		"\u00e3\u0003\u001e\u000f\u0000\u00df\u00e0\u0005!\u0000\u0000\u00e0\u00e2"+
		"\u0003\u001e\u000f\u0000\u00e1\u00df\u0001\u0000\u0000\u0000\u00e2\u00e5"+
		"\u0001\u0000\u0000\u0000\u00e3\u00e1\u0001\u0000\u0000\u0000\u00e3\u00e4"+
		"\u0001\u0000\u0000\u0000\u00e4\u00e8\u0001\u0000\u0000\u0000\u00e5\u00e3"+
		"\u0001\u0000\u0000\u0000\u00e6\u00e7\u0005\u0003\u0000\u0000\u00e7\u00e9"+
		"\u0003\f\u0006\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000\u00e8\u00e9\u0001"+
		"\u0000\u0000\u0000\u00e9\u001d\u0001\u0000\u0000\u0000\u00ea\u00eb\u0005"+
		"\'\u0000\u0000\u00eb\u00ed\u0005 \u0000\u0000\u00ec\u00ea\u0001\u0000"+
		"\u0000\u0000\u00ec\u00ed\u0001\u0000\u0000\u0000\u00ed\u00ee\u0001\u0000"+
		"\u0000\u0000\u00ee\u00ef\u0005\'\u0000\u0000\u00ef\u00f0\u0005\u0019\u0000"+
		"\u0000\u00f0\u00f1\u0003\u0012\t\u0000\u00f1\u001f\u0001\u0000\u0000\u0000"+
		"\u00f2\u00f3\u0005\u0011\u0000\u0000\u00f3\u00f4\u0005\u0002\u0000\u0000"+
		"\u00f4\u00f5\u0005\u0015\u0000\u0000\u00f5\u00f8\u0003(\u0014\u0000\u00f6"+
		"\u00f7\u0005\u0003\u0000\u0000\u00f7\u00f9\u0003\f\u0006\u0000\u00f8\u00f6"+
		"\u0001\u0000\u0000\u0000\u00f8\u00f9\u0001\u0000\u0000\u0000\u00f9!\u0001"+
		"\u0000\u0000\u0000\u00fa\u00fb\u0005\u0012\u0000\u0000\u00fb\u00fc\u0005"+
		"\u0015\u0000\u0000\u00fc\u00fd\u0005\u0014\u0000\u0000\u00fd\u00fe\u0003"+
		"(\u0014\u0000\u00fe\u00ff\u0005\"\u0000\u0000\u00ff\u0104\u0003$\u0012"+
		"\u0000\u0100\u0101\u0005!\u0000\u0000\u0101\u0103\u0003$\u0012\u0000\u0102"+
		"\u0100\u0001\u0000\u0000\u0000\u0103\u0106\u0001\u0000\u0000\u0000\u0104"+
		"\u0102\u0001\u0000\u0000\u0000\u0104\u0105\u0001\u0000\u0000\u0000\u0105"+
		"\u0107\u0001\u0000\u0000\u0000\u0106\u0104\u0001\u0000\u0000\u0000\u0107"+
		"\u0108\u0005#\u0000\u0000\u0108#\u0001\u0000\u0000\u0000\u0109\u010a\u0005"+
		"\'\u0000\u0000\u010a\u010b\u0005\'\u0000\u0000\u010b\u010c\u0005\u0016"+
		"\u0000\u0000\u010c\u010f\u0005\'\u0000\u0000\u010d\u010e\u0005\u0017\u0000"+
		"\u0000\u010e\u0110\u0005\u0018\u0000\u0000\u010f\u010d\u0001\u0000\u0000"+
		"\u0000\u010f\u0110\u0001\u0000\u0000\u0000\u0110%\u0001\u0000\u0000\u0000"+
		"\u0111\u0112\u0005\u0013\u0000\u0000\u0112\u0113\u0005\u0015\u0000\u0000"+
		"\u0113\u0114\u0005\u0014\u0000\u0000\u0114\u0115\u0003(\u0014\u0000\u0115"+
		"\'\u0001\u0000\u0000\u0000\u0116\u0117\u0005\'\u0000\u0000\u0117)\u0001"+
		"\u0000\u0000\u0000\u0118\u0119\u0005\'\u0000\u0000\u0119+\u0001\u0000"+
		"\u0000\u0000\u011a\u011b\u0005\'\u0000\u0000\u011b-\u0001\u0000\u0000"+
		"\u0000\u001cCLQT\\_cflosvx\u0088\u0090\u0092\u009f\u00aa\u00b3\u00be\u00ca"+
		"\u00d5\u00e3\u00e8\u00ec\u00f8\u0104\u010f";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}