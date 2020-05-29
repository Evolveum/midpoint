/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.util.Set;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.api.stream.AxiomItemStream.Target;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;

public class AxiomAntlrVisitor<T> extends AbstractAxiomAntlrVisitor<T> {

    private final AxiomIdentifierResolver statements;
    private final AxiomIdentifierResolver arguments;
    private final AxiomItemStream.Target delegate;

    public AxiomAntlrVisitor(String name, AxiomIdentifierResolver statements, AxiomIdentifierResolver arguments, AxiomItemStream.Target delegate,
            Set<AxiomName> limit) {
        super(name, limit);
        this.statements = statements;
        this.arguments = arguments;
        this.delegate = delegate;
    }

    @Override
    protected AxiomName resolveArgument(String prefix, String localName) {
        return arguments.resolveIdentifier(prefix, localName);
    }

    @Override
    protected AxiomName resolveItemName(String prefix, String localName) {
        return statements.resolveIdentifier(prefix, localName);
    }

    @Override
    protected Target delegate() {
        return delegate;
    }

}
