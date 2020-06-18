/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

class AxiomMapItemImpl<V> extends AxiomItemImpl<V> implements AxiomMapItem<V> {

    private final Map<AxiomValueIdentifier, AxiomValue<V>> values;

    private AxiomMapItemImpl(AxiomItemDefinition definition, Collection<? extends AxiomValue<V>> val) {
        super(definition, ImmutableList.copyOf(val));
        Preconditions.checkArgument(definition.identifierDefinition().isPresent(), "Item must have identifier defined.");

        // FIXME: Should be offset map?
        Builder<AxiomValueIdentifier, AxiomValue<V>> builder = ImmutableMap.builder();
        for(AxiomValue<V> value :val) {
            builder.put(AxiomValueIdentifier.from(definition.identifierDefinition().get(), value), value);
        }
        values = builder.build();
    }

    @Override
    public Optional<? extends AxiomValue<V>> value(AxiomValueIdentifier key) {
        return Optional.ofNullable(values.get(key));
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition definition, Collection<? extends AxiomValue<V>> values) {
        return new AxiomMapItemImpl<>(definition, values);
    }

}
