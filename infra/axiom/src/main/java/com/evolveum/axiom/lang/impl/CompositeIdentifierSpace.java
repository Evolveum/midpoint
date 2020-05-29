package com.evolveum.axiom.lang.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;

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
    public ValueContext<?> lookup(AxiomName space, IdentifierSpaceKey key) {
        for (IdentifierSpaceHolder delegate : delegates) {
            ValueContext<?>  maybe = delegate.lookup(space, key);
            if(maybe != null) {
                return maybe;
            }
        }
        return null;
    }

    @Override
    public void register(AxiomName space, Scope scope, IdentifierSpaceKey key, ValueContext<?> context) {
        export.register(space, scope, key, context);
    }

    @Override
    public Map<IdentifierSpaceKey, ValueContext<?>> space(AxiomName space) {
        throw new UnsupportedOperationException();
    }

    public void add(IdentifierSpaceHolder holder) {
        delegates.add(holder);
    }

}
