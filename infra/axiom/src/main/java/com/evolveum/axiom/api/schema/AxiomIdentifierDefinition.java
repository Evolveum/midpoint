/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import java.util.Collection;
import java.util.Set;

import com.evolveum.axiom.api.AxiomStructuredValue;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.google.common.collect.ImmutableSet;

public interface AxiomIdentifierDefinition extends AxiomStructuredValue {

    Collection<AxiomName> components();

    enum Scope {
        GLOBAL,
        PARENT,
        LOCAL
    }

    static AxiomIdentifierDefinition global(AxiomName name, AxiomName... components) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(components), name, Scope.GLOBAL);
    }

    static AxiomIdentifierDefinition local(AxiomName name, AxiomName... components) {
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

    static AxiomIdentifierDefinition from(AxiomName space, Scope scope, Set<AxiomName> members) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(members), space, scope);
    }

    static AxiomIdentifierDefinition parent(AxiomName name, AxiomName... components) {
        return new AxiomIdentifierDefinitionImpl(ImmutableSet.copyOf(components), name, Scope.PARENT);
    }

}
