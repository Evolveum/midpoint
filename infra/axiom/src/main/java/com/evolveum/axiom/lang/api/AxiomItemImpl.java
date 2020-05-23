package com.evolveum.axiom.lang.api;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

class AxiomItemImpl<V> extends AbstractAxiomItem<V> {

    Collection<AxiomItemValue<V>> values;


    private AxiomItemImpl(AxiomItemDefinition definition, Collection<? extends AxiomItemValue<V>> val) {
        super(definition);
        this.values = ImmutableList.copyOf(val);
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition definition, Collection<? extends AxiomItemValue<V>> values) {
        return new AxiomItemImpl<>(definition, values);
    }

    @Override
    public Collection<AxiomItemValue<V>> values() {
        return values;
    }

}
