package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

public class AxiomErrorListener extends BaseErrorListener {

    private final String source;
    private final List<AxiomSyntaxException> exceptions = new ArrayList<>();

    public AxiomErrorListener(String source) {
        this.source = source;
    }

    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
            final int charPositionInLine, final String msg, final RecognitionException e) {
        // TODO Auto-generated method stub

    }

    public void validate() throws AxiomSyntaxException {
        if (exceptions.isEmpty()) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (AxiomSyntaxException e : exceptions) {
            if (first) {
                first = false;
            } else {
                sb.append('\n');
            }

            sb.append(e.getFormattedMessage());
        }
        throw new AxiomSyntaxException(source, 0, 0, sb.toString());
    }
}
