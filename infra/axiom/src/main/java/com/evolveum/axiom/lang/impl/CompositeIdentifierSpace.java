/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.api.AxiomValueIdentifier;

class CompositeIdentifierSpace implements IdentifierSpaceHolder, NamespaceContext {

    private final Set<IdentifierSpaceHolder> delegates = new HashSet<>();
    private final IdentifierSpaceHolderImpl export = new IdentifierSpaceHolderImpl(Scope.GLOBAL);



    public IdentifierSpaceHolderImpl getExport() {
        return export;
    }

    public CompositeIdentifierSpace() {
        delegates.add(export);
    }

    @Override
    public ValueContext<?> lookup(AxiomName space, AxiomValueIdentifier key) {
        for (IdentifierSpaceHolder delegate : delegates) {
            ValueContext<?>  maybe = delegate.lookup(space, key);
            if(maybe != null) {
                return maybe;
            }
        }
        return null;
    }

    @Override
    public void register(AxiomName space, Scope scope, AxiomValueIdentifier key, ValueContext<?> context) {
        export.register(space, scope, key, context);
    }

    @Override
    public Map<AxiomValueIdentifier, ValueContext<?>> space(AxiomName space) {
        throw new UnsupportedOperationException();
    }

    public void add(IdentifierSpaceHolder holder) {
        delegates.add(holder);
    }

}
