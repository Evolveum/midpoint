package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public class ItemValueImpl<V> implements AxiomValue<V> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final AxiomValueFactory FACTORY = ItemValueImpl::new;
    private final AxiomTypeDefinition type;
    private final V value;
    private final Map<AxiomName, AxiomItem<?>> items;


    protected <X> X require(Optional<X> value) {
        return value.get();
    }

    public ItemValueImpl(AxiomTypeDefinition type, V value, Map<AxiomName, AxiomItem<?>> items) {
        super();
        this.type = type;
        this.value = value;
        this.items = items;
    }

    @SuppressWarnings("unchecked")
    public static <V> AxiomValueFactory<V,AxiomValue<V>> factory() {
        return FACTORY;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public Optional<AxiomItem<?>> item(AxiomItemDefinition def) {
        return AxiomValue.super.item(def);
    }

    @Override
    public <T> Optional<AxiomItem<T>> item(AxiomName name) {
        return Optional.ofNullable((AxiomItem<T>) items.get(name));
    }

    @Override
    public Collection<AxiomItem<?>> items() {
        return items.values();
    }
}
