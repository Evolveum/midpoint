package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
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
    public <T> Optional<AxiomItem<T>> item(AxiomName name) {
        return Optional.ofNullable((AxiomItem<T>) items.get(name));
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
}
