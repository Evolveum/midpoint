package com.evolveum.axiom.api.schema;

import java.util.Optional;
import java.util.Set;

import com.evolveum.axiom.api.AxiomName;
import com.google.common.collect.ImmutableSet;

class AxiomIdentifierDefinitionImpl implements AxiomIdentifierDefinition {

    private Set<AxiomItemDefinition> components;

    private AxiomName space;

    private Scope scope;

    public AxiomIdentifierDefinitionImpl(Set<AxiomItemDefinition> components, AxiomName space, Scope scope) {
        super();
        this.components = ImmutableSet.copyOf(components);
        this.space = space;
        this.scope = scope;
    }

    @Override
    public Set<AxiomItemDefinition> components() {
        return components;
    }

    @Override
    public Scope scope() {
        return scope;
    }

    @Override
    public AxiomName space() {
        return space;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        // TODO Auto-generated method stub
        return null;
    }

}
