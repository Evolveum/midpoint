package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.collect.ImmutableSet;

class AxiomIdentifierDefinitionImpl implements AxiomIdentifierDefinition {

    private Set<AxiomItemDefinition> components;

    private AxiomIdentifier space;

    private Scope scope;

    public AxiomIdentifierDefinitionImpl(Set<AxiomItemDefinition> components, AxiomIdentifier space, Scope scope) {
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
    public AxiomIdentifier space() {
        return space;
    }

}
