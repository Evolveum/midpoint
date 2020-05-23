package com.evolveum.axiom.lang.api;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;

public abstract class AbstractAxiomItem<V> implements AxiomItem<V> {


    private final AxiomItemDefinition definition;

    public AbstractAxiomItem(AxiomItemDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Optional<AxiomItemDefinition> definition() {
        return Optional.of(definition);
    }

    @Override
    public AxiomIdentifier name() {
        return definition.name();
    }
}
