package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public class ItemValueImpl implements AxiomComplexValue {

    private static final AxiomValueFactory<Collection<AxiomItem<?>>, AxiomComplexValue> FACTORY = ItemValueImpl::new;
    private final AxiomTypeDefinition type;
    private final Map<AxiomName, AxiomItem<?>> items;


    protected <X> X require(Optional<X> value) {
        return value.get();
    }

    public ItemValueImpl(AxiomTypeDefinition type, Collection<AxiomItem<?>> value, Map<AxiomName, AxiomItem<?>> items) {
        super();
        this.type = type;
        this.items = items;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <V> AxiomValueFactory<V,AxiomValue<V>> factory() {
        return (AxiomValueFactory) FACTORY;
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
}
