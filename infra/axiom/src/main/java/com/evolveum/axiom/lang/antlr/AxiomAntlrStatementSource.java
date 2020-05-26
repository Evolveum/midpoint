/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.antlr.AxiomParser.StatementContext;
import com.evolveum.axiom.lang.api.AxiomItemStream;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class AxiomAntlrStatementSource {

    private final StatementContext root;
    private final String sourceName;

    public static AxiomAntlrStatementSource from(String sourceName, InputStream stream) throws IOException, AxiomSyntaxException {
        return from(sourceName, CharStreams.fromStream(stream));
    }

    public static StatementContext contextFrom(String sourceName, CharStream stream) {
        AxiomLexer lexer = new AxiomLexer(stream);
        AxiomParser parser = new AxiomParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        AxiomErrorListener errorListener = new AxiomErrorListener(sourceName);
        parser.addErrorListener(errorListener);
        StatementContext statement = parser.statement();
        errorListener.validate();
        return statement;
    }

    public static AxiomAntlrStatementSource from(String sourceName, CharStream stream) throws AxiomSyntaxException {
        StatementContext statement = contextFrom(sourceName, stream);
        return new AxiomAntlrStatementSource(sourceName, statement);
    }

    protected AxiomAntlrStatementSource(String sourceName, StatementContext statement) {
        this.sourceName = sourceName;
        this.root = statement;
    }

    public String sourceName() {
        return sourceName;
    }

    protected final StatementContext root() {
        return root;
    }

    public final void stream(AxiomItemStream.TargetWithResolver target, Optional<Set<AxiomIdentifier>> emitOnly) {
        AxiomAntlrVisitor2<?> visitor = new AxiomAntlrVisitor2<>(sourceName, target, emitOnly.orElse(null));
        visitor.visit(root);
    }

    public final void stream(AxiomIdentifierResolver statements, AxiomIdentifierResolver arguments, AxiomItemStream.Target listener,
            Optional<Set<AxiomIdentifier>> emitOnly) {
        AxiomAntlrVisitor<?> visitor = new AxiomAntlrVisitor<>(sourceName, statements, arguments, listener, emitOnly.orElse(null));
        visitor.visit(root);
    }

}
