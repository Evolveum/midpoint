package com.evolveum.axiom.lang.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class AxiomItemBuilder<V> implements Supplier<AxiomItem<V>> {

    Collection<Supplier<? extends AxiomValue<V>>> values = new ArrayList<>();
    private AxiomItemDefinition definition;

    public AxiomItemBuilder(AxiomItemDefinition definition) {
        this.definition = definition;
    }

    public AxiomItemDefinition definition() {
        return definition;
    }

    public void addValue(Supplier<? extends AxiomValue<V>> value) {
        values.add(value);
    }

    @Override
    public AxiomItem<V> get() {
        Builder<AxiomValue<V>> result = ImmutableList.builder();
        for(Supplier<? extends AxiomValue<V>> value : values) {
            result.add(value.get());
        }
        return AxiomItem.from(definition, result.build());
    }


}
