package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;

class CompactAxiomItem<V> extends AbstractAxiomItem<V> implements AxiomValue<V> {

    private final V value;

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return definition().map(AxiomItemDefinition::typeDefinition);
    }

    private CompactAxiomItem(AxiomItemDefinition definition, V value) {
        super(definition);
        this.value = value;
    }

    public static <V> AxiomItem<V> of(AxiomItemDefinition def, V value) {
        return new CompactAxiomItem<V>(def, value);
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public Collection<AxiomValue<V>> values() {
        return ImmutableSet.of(this);
    }

    @Override
    public AxiomValue<V> onlyValue() {
        return this;
    }


}
