package com.evolveum.axiom.api;

import java.util.Optional;

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
