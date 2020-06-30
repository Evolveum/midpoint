/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomItem;

abstract class DelegatedItemDefinition implements AxiomItemDefinition {

    protected abstract AxiomItemDefinition delegate();

    @Override
    public boolean operational() {
        return false;
    }


    @Override
    public AxiomName name() {
        return delegate().name();
    }

    @Override
    public String documentation() {
        return delegate().documentation();
    }

    @Override
    public AxiomTypeDefinition typeDefinition() {
        return delegate().typeDefinition();
    }

    @Override
    public boolean required() {
        return delegate().required();
    }

    @Override
    public int minOccurs() {
        return delegate().minOccurs();
    }

    @Override
    public int maxOccurs() {
        return delegate().maxOccurs();
    }

    @Override
    public String toString() {
        return AxiomItemDefinition.toString(this);
    }

    @Override
    public AxiomTypeDefinition definingType() {
        return delegate().definingType();
    }

    @Override
    public Optional<AxiomIdentifierDefinition> identifierDefinition() {
        return delegate().identifierDefinition();
    }

    @Override
    public Optional<AxiomName> substitutionOf() {
        return delegate().substitutionOf();
    }

    @Override
    public Optional<AxiomValue<?>> constantValue() {
        return delegate().constantValue();
    }

    @Override
    public Optional<AxiomValue<?>> defaultValue() {
        return delegate().defaultValue();
    }
}
