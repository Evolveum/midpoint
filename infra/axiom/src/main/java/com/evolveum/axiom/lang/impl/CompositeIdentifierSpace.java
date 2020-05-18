package com.evolveum.axiom.lang.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;

public class CompositeIdentifierSpace implements IdentifierSpaceHolder, NamespaceContext {

    private final Set<IdentifierSpaceHolder> delegates = new HashSet<>();

    @Override
    public StatementContextImpl<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key) {
        for (IdentifierSpaceHolder delegate : delegates) {
            StatementContextImpl<?>  maybe = delegate.lookup(space, key);
            if(maybe != null) {
                return maybe;
            }
        }
        return null;
    }

    @Override
    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, StatementContextImpl<?> context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<IdentifierSpaceKey, StatementContextImpl<?>> space(AxiomIdentifier space) {
        throw new UnsupportedOperationException();
    }

    public void add(IdentifierSpaceHolder holder) {
        delegates.add(holder);
    }

}
