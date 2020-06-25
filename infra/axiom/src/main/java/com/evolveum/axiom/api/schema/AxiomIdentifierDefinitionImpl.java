/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomName;
import com.google.common.collect.ImmutableSet;

class AxiomIdentifierDefinitionImpl implements AxiomIdentifierDefinition {

    private Set<AxiomName> components;


    public AxiomIdentifierDefinitionImpl(Set<AxiomName> components, AxiomName space, Scope scope) {
        super();
        this.components = ImmutableSet.copyOf(components);
    }

    @Override
    public Set<AxiomName> components() {
        return components;
    }
    @Override
    public Optional<AxiomTypeDefinition> type() {
        return null;
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> itemMap() {
        return Collections.emptyMap();
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> infraItems() {
        return null;
    }
}
