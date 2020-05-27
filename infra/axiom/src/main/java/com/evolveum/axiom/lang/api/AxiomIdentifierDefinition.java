package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomValue;
import com.google.common.collect.ImmutableSet;

public interface AxiomIdentifierDefinition extends AxiomValue<AxiomIdentifierDefinition> {

    @Override
    default AxiomIdentifierDefinition get() {
        return this;
    }

    Collection<AxiomItemDefinition> components();

    Scope scope();

    AxiomIdentifier space();

    enum Scope {
        GLOBAL,
        PARENT,
        LOCAL
    }

    static AxiomIdentifierDefinition global(AxiomIdentifier name, AxiomItemDefinition... components) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(components), name, Scope.GLOBAL);
    }

    static AxiomIdentifierDefinition local(AxiomIdentifier name, AxiomItemDefinition... components) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(components), name, Scope.LOCAL);
    }

    static Scope scope(String scope) {
        if(Scope.GLOBAL.name().equalsIgnoreCase(scope)) {
            return Scope.GLOBAL;
        }
        if(Scope.PARENT.name().equalsIgnoreCase(scope)) {
            return Scope.PARENT;
        }
        if(Scope.LOCAL.name().equalsIgnoreCase(scope)) {
            return Scope.LOCAL;
        }
        throw new IllegalArgumentException("Unknown scope " + scope);
    }

    static AxiomIdentifierDefinition from(AxiomIdentifier space, Scope scope, Set<AxiomItemDefinition> members) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(members), space, scope);
    }

    static AxiomIdentifierDefinition parent(AxiomIdentifier name, AxiomItemDefinition... components) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(components), name, Scope.PARENT);
    }

}
