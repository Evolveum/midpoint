/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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

    public Supplier<? extends AxiomValue<V>> onlyValue() {
        return Iterables.getOnlyElement(values);
    }


}
