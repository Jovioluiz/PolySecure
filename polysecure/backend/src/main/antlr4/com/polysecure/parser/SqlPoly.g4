grammar SqlPoly;

query
    : selectStatement       EOF
    | insertStatement       EOF
    | insertSelectStatement EOF
    | updateStatement       EOF
    | deleteStatement       EOF
    | createTableStatement  EOF
    | dropTableStatement    EOF
    ;

// ─── SELECT ──────────────────────────────────────────────────────────────────

selectStatement
    : K_SELECT selectList
      K_FROM tableRef
      joinClause*
      (K_WHERE condition)?
      SEMICOLON?
    ;

selectList : STAR | selectItem (COMMA selectItem)* ;

selectItem : expr (K_AS? alias=IDENTIFIER)? ;

tableRef
    : store=IDENTIFIER DOT table=IDENTIFIER (K_AS? alias=IDENTIFIER)?   # CrossStoreTableRef
    | table=IDENTIFIER (K_AS? alias=IDENTIFIER)?                          # LocalTableRef
    ;

joinClause : K_JOIN tableRef K_ON condition ;

condition
    : LPAREN condition RPAREN                                              # ParenCondition
    | K_NOT condition                                                      # NotCondition
    | left=condition K_OR  right=condition                                 # OrCondition
    | left=condition K_AND right=condition                                 # AndCondition
    | left=expr op=compOp  right=expr                                      # CompareCondition
    | expr K_NOT K_IN  LPAREN literal (COMMA literal)* RPAREN             # NotInCondition
    | expr K_IN        LPAREN literal (COMMA literal)* RPAREN             # InCondition
    | expr K_IS K_NOT K_NULL                                               # IsNotNullCondition
    | expr K_IS K_NULL                                                     # IsNullCondition
    | expr K_NOT K_LIKE val=STRING_LITERAL                                 # NotLikeCondition
    | expr K_LIKE val=STRING_LITERAL                                       # LikeCondition
    | expr K_BETWEEN low=literal K_AND high=literal                        # BetweenCondition
    ;

compOp : EQ | NEQ | GT | LT | GTE | LTE ;

expr
    : qualifier=IDENTIFIER DOT col=IDENTIFIER              # QualifiedColumn
    | qualifier=IDENTIFIER DOT STAR                        # QualifiedStar
    | name=IDENTIFIER                                      # SimpleColumn
    | val=literal                                          # LiteralVal
    ;

literal : STRING_LITERAL | NUMBER | K_TRUE | K_FALSE | K_NULL ;

// ─── INSERT (values) ─────────────────────────────────────────────────────────

insertStatement
    : K_INSERT K_INTO K_POLYSTORE tableName storeInsertClause+
    ;

storeInsertClause
    : storeName LPAREN columnName (COMMA columnName)* RPAREN
      K_VALUES LPAREN literal (COMMA literal)* RPAREN
    ;

// ─── INSERT … SELECT ─────────────────────────────────────────────────────────

insertSelectStatement
    : K_INSERT K_INTO K_POLYSTORE tableName storeTargetClause+ selectStatement
    ;

storeTargetClause
    : storeName LPAREN columnName (COMMA columnName)* RPAREN
    ;

// ─── UPDATE ──────────────────────────────────────────────────────────────────

updateStatement
    : K_UPDATE K_POLYSTORE tableName
      K_SET setClause (COMMA setClause)*
      (K_WHERE condition)?
    ;

setClause
    : (store=IDENTIFIER DOT)? col=IDENTIFIER EQ val=literal
    ;

// ─── DELETE ──────────────────────────────────────────────────────────────────

deleteStatement
    : K_DELETE K_FROM K_POLYSTORE tableName
      (K_WHERE condition)?
    ;

// ─── CREATE TABLE ────────────────────────────────────────────────────────────

createTableStatement
    : K_CREATE K_POLYSTORE K_TABLE tableName
      LPAREN columnDef (COMMA columnDef)* RPAREN
    ;

columnDef
    : name=IDENTIFIER type=IDENTIFIER K_STORE store=IDENTIFIER (K_PRIMARY K_KEY)?
    ;

// ─── DROP TABLE ──────────────────────────────────────────────────────────────

dropTableStatement
    : K_DROP K_POLYSTORE K_TABLE tableName
    ;

tableName  : IDENTIFIER ;
storeName  : IDENTIFIER ;
columnName : IDENTIFIER ;

// ─── KEYWORDS ────────────────────────────────────────────────────────────────

K_SELECT    : S E L E C T ;
K_FROM      : F R O M ;
K_WHERE     : W H E R E ;
K_JOIN      : J O I N ;
K_ON        : O N ;
K_AND       : A N D ;
K_OR        : O R ;
K_AS        : A S ;
K_NULL      : N U L L ;
K_TRUE      : T R U E ;
K_FALSE     : F A L S E ;
K_INSERT    : I N S E R T ;
K_INTO      : I N T O ;
K_VALUES    : V A L U E S ;
K_UPDATE    : U P D A T E ;
K_SET       : S E T ;
K_DELETE    : D E L E T E ;
K_CREATE    : C R E A T E ;
K_DROP      : D R O P ;
K_TABLE     : T A B L E ;
K_POLYSTORE : P O L Y S T O R E ;
K_STORE     : S T O R E ;
K_PRIMARY   : P R I M A R Y ;
K_KEY       : K E Y ;
K_NOT       : N O T ;
K_IN        : I N ;
K_IS        : I S ;
K_LIKE      : L I K E ;
K_BETWEEN   : B E T W E E N ;

EQ        : '=' ;
NEQ       : '!=' | '<>' ;
GT        : '>' ;
LT        : '<' ;
GTE       : '>=' ;
LTE       : '<=' ;
STAR      : '*' ;
DOT       : '.' ;
COMMA     : ',' ;
LPAREN    : '(' ;
RPAREN    : ')' ;
SEMICOLON : ';' ;

STRING_LITERAL : '\'' (~'\'' | '\'\'')* '\'' ;
NUMBER         : '-'? [0-9]+ ('.' [0-9]+)? ;
IDENTIFIER     : [a-zA-Z_][a-zA-Z0-9_]* ;
LINE_COMMENT   : '--' ~[\r\n]* -> skip ;
BLOCK_COMMENT  : '/*' .*? '*/' -> skip ;
WS             : [ \t\r\n]+ -> skip ;

fragment A:[Aa]; fragment B:[Bb]; fragment C:[Cc]; fragment D:[Dd]; fragment E:[Ee];
fragment F:[Ff]; fragment G:[Gg]; fragment H:[Hh]; fragment I:[Ii]; fragment J:[Jj];
fragment K:[Kk]; fragment L:[Ll]; fragment M:[Mm]; fragment N:[Nn]; fragment O:[Oo];
fragment P:[Pp]; fragment Q:[Qq]; fragment R:[Rr]; fragment S:[Ss]; fragment T:[Tt];
fragment U:[Uu]; fragment V:[Vv]; fragment W:[Ww]; fragment X:[Xx]; fragment Y:[Yy];
fragment Z:[Zz];
