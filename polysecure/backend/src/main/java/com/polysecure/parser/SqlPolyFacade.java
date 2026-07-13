/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.parser;

import com.polysecure.model.Statement;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;

@Component
public class SqlPolyFacade {

    public Statement parse(String sql) {
        SqlPolyLexer lexer = new SqlPolyLexer(CharStreams.fromString(sql));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SqlPolyParser parser = new SqlPolyParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        ParseTree tree = parser.query();
        return (Statement) new SqlPolyVisitorImpl().visit(tree);
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new ParseException("Syntax error at line " + line + ":" + charPositionInLine + " — " + msg);
        }
    }
}
