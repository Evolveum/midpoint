package com.evolveum.axiom.api.schema;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;

abstract class DelegatedItemDefinition implements AxiomItemDefinition {

    protected abstract AxiomItemDefinition delegate();

    @Override
    public boolean operational() {
        return false;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return delegate().type();
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> itemMap() {
        return delegate().itemMap();
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
    public Optional<AxiomItem<?>> item(AxiomItemDefinition def) {
        return delegate().asComplex().get().item(def);
    }

    @Override
    public <T> Optional<AxiomItem<T>> item(AxiomName name) {
        return delegate().asComplex().get().item(name);
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
    public Map<AxiomName, AxiomItem<?>> infraItems() {
        return delegate().infraItems();
    }

    @Override
    public Optional<AxiomName> substitutionOf() {
        return delegate().substitutionOf();
    }
}
