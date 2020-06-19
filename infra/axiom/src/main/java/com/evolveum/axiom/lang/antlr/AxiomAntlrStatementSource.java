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

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.api.stream.AxiomItemStream.TargetWithResolver;
import com.evolveum.axiom.api.stream.AxiomStreamTarget;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemContext;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class AxiomAntlrStatementSource {

    private final ItemContext root;
    private final String sourceName;

    public static AxiomAntlrStatementSource from(String sourceName, InputStream stream) throws IOException, AxiomSyntaxException {
        return from(sourceName, CharStreams.fromStream(stream));
    }

    public static ItemContext contextFrom(String sourceName, CharStream stream) {
        AxiomLexer lexer = new AxiomLexer(stream);
        AxiomParser parser = new AxiomParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        AxiomErrorListener errorListener = new AxiomErrorListener(sourceName);
        parser.addErrorListener(errorListener);
        ItemContext statement = parser.item();
        errorListener.validate();
        return statement;
    }

    public static AxiomAntlrStatementSource from(String sourceName, CharStream stream) throws AxiomSyntaxException {
        ItemContext statement = contextFrom(sourceName, stream);
        return new AxiomAntlrStatementSource(sourceName, statement);
    }

    protected AxiomAntlrStatementSource(String sourceName, ItemContext statement) {
        this.sourceName = sourceName;
        this.root = statement;
    }

    public String sourceName() {
        return sourceName;
    }

    protected final ItemContext root() {
        return root;
    }

    public final void stream(AxiomItemStream.TargetWithResolver target) {
        stream(target, Optional.empty());
    }

    public void stream(AxiomItemStream.TargetWithResolver target, Optional<Set<AxiomName>> emitOnly) {
        stream(target, emitOnly, AxiomNameResolver.nullResolver());
    }

    public final void stream(TargetWithResolver target, Optional<Set<AxiomName>> emitOnly,
            AxiomNameResolver resolver) {
        AxiomStreamTarget<AxiomPrefixedName> prefixedTarget = target.asPrefixed(resolver);
        AxiomAntlrVisitor2<?> visitor = new AxiomAntlrVisitor2<>(sourceName, prefixedTarget);
        visitor.visit(root);
    }

}
