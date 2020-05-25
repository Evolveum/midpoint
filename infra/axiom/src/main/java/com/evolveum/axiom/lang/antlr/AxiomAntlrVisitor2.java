/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemStream;
import com.evolveum.axiom.lang.api.AxiomItemStream.Target;

public class AxiomAntlrVisitor2<T> extends AbstractAxiomAntlrVisitor<T> {

    private final AxiomItemStream.TargetWithResolver delegate;

    public AxiomAntlrVisitor2(String name, AxiomItemStream.TargetWithResolver delegate,
            Set<AxiomIdentifier> limit) {
        super(name, limit);
        this.delegate = delegate;
    }

    @Override
    protected AxiomIdentifier resolveArgument(String prefix, String localName) {
        return delegate.valueResolver().resolveIdentifier(prefix, localName);
    }

    @Override
    protected AxiomIdentifier resolveItemName(String prefix, String localName) {
        return delegate.itemResolver().resolveIdentifier(prefix, localName);
    }

    @Override
    protected Target delegate() {
        return delegate;
    }
}
