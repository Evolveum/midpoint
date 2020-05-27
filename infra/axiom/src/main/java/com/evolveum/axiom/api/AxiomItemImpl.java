package com.evolveum.axiom.api;

import java.util.Collection;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.google.common.collect.ImmutableList;

class AxiomItemImpl<V> extends AbstractAxiomItem<V> {

    Collection<AxiomValue<V>> values;


    private AxiomItemImpl(AxiomItemDefinition definition, Collection<? extends AxiomValue<V>> val) {
        super(definition);
        this.values = ImmutableList.copyOf(val);
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition definition, Collection<? extends AxiomValue<V>> values) {
        return new AxiomItemImpl<>(definition, values);
    }

    @Override
    public Collection<AxiomValue<V>> values() {
        return values;
    }

}
