/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;

class DefaultTypeDefinition implements AxiomTypeDefinition {

    private AxiomIdentifier identifier;
    private Map<AxiomIdentifier, AxiomItemDefinition> items;

    public DefaultTypeDefinition(AxiomIdentifier identifier, Map<AxiomIdentifier, AxiomItemDefinition> items) {
        super();
        this.identifier = identifier;
        this.items = items;
    }

    @Override
    public AxiomIdentifier name() {
        return identifier;
    }

    @Override
    public String documentation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<AxiomItemDefinition> argument() {
        return Optional.empty();
    }

    @Override
    public Optional<AxiomTypeDefinition> superType() {
        return Optional.empty();
    }

    @Override
    public Map<AxiomIdentifier, AxiomItemDefinition> items() {
        return items;
    }



}
