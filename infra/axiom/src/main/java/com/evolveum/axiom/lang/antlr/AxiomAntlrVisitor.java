/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.api.stream.AxiomItemStream.Target;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;

public class AxiomAntlrVisitor<T> extends AbstractAxiomAntlrVisitor<T> {

    private final AxiomIdentifierResolver statements;
    private final AxiomIdentifierResolver arguments;
    private final AxiomItemStream.Target delegate;

    public AxiomAntlrVisitor(String name, AxiomIdentifierResolver statements, AxiomIdentifierResolver arguments, AxiomItemStream.Target delegate,
            Set<AxiomIdentifier> limit) {
        super(name, limit);
        this.statements = statements;
        this.arguments = arguments;
        this.delegate = delegate;
    }

    @Override
    protected AxiomIdentifier resolveArgument(String prefix, String localName) {
        return arguments.resolveIdentifier(prefix, localName);
    }

    @Override
    protected AxiomIdentifier resolveItemName(String prefix, String localName) {
        return statements.resolveIdentifier(prefix, localName);
    }

    @Override
    protected Target delegate() {
        return delegate;
    }

}
