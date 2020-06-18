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
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class ComplexValueImpl implements AxiomComplexValue {

    private final AxiomTypeDefinition type;
    private final Map<AxiomName, AxiomItem<?>> items;
    private final Map<AxiomName, AxiomItem<?>> infraItems;



    protected <X> X require(Optional<X> value) {
        return value.get();
    }

    public ComplexValueImpl(AxiomTypeDefinition type, Map<AxiomName, AxiomItem<?>> items, Map<AxiomName,AxiomItem<?>> infraItems) {
        super();
        this.type = type;
        this.items = ImmutableMap.copyOf(items);
        this.infraItems = ImmutableMap.copyOf(infraItems);
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public Optional<AxiomItem<?>> item(AxiomItemDefinition def) {
        return AxiomComplexValue.super.item(def);
    }

    @Override
    public Optional<? extends AxiomItem<?>> item(AxiomName name) {
        return Optional.ofNullable(items.get(name));
    }

    protected AxiomItem<?> requireItem(AxiomName name) {
        return item(name).orElseThrow(() -> new IllegalStateException(Strings.lenientFormat("Required item %s not present.", name)));
    }

    public Collection<AxiomItem<?>> items() {
        return items.values();
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> itemMap() {
        return items;
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> infraItems() {
        return infraItems;
    }

    @SuppressWarnings("unchecked")
    protected <T> Optional<AxiomItem<T>> as(Class<T> type, Optional<? extends AxiomItem<?>> item) {
        return (Optional<AxiomItem<T>>) item;
    }
}
