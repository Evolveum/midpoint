/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.stream.AxiomStreamTarget;

public class AxiomAntlrVisitor2<T> extends AbstractAxiomAntlrVisitor<T> {

    private final AxiomStreamTarget<AxiomPrefixedName, Object> delegate;

    public AxiomAntlrVisitor2(String name, AxiomStreamTarget<AxiomPrefixedName, Object> delegate) {
        super(name);
        this.delegate = delegate;
    }

    @Override
    protected AxiomStreamTarget<AxiomPrefixedName, Object> delegate() {
        return delegate;
    }
}
