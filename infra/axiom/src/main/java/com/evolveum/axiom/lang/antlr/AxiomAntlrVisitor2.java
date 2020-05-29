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

public class AxiomAntlrVisitor2<T> extends AbstractAxiomAntlrVisitor<T> {

    private final AxiomItemStream.TargetWithResolver delegate;

    public AxiomAntlrVisitor2(String name, AxiomItemStream.TargetWithResolver delegate,
            Set<AxiomName> limit) {
        super(name, limit);
        this.delegate = delegate;
    }

    @Override
    protected AxiomName resolveArgument(String prefix, String localName) {
        return delegate.valueResolver().resolveIdentifier(prefix, localName);
    }

    @Override
    protected AxiomName resolveItemName(String prefix, String localName) {
        return delegate.itemResolver().resolveIdentifier(prefix, localName);
    }

    @Override
    protected Target delegate() {
        return delegate;
    }
}
