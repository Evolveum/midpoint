/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class AxiomErrorListener extends BaseErrorListener {

    private final String source;
    private final List<AxiomSyntaxException> exceptions = new ArrayList<>();

    public AxiomErrorListener(String source) {
        this.source = source;
    }

    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
            final int charPositionInLine, final String msg, final RecognitionException e) {
        exceptions.add(new AxiomSyntaxException(source, line, charPositionInLine, msg));
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
