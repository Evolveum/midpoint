package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomValue;
import com.evolveum.axiom.lang.api.AxiomValueFactory;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;

public class ItemValueImpl<V> implements AxiomValue<V> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final AxiomValueFactory FACTORY = ItemValueImpl::new;
    private final AxiomTypeDefinition type;
    private final V value;
    private final Map<AxiomIdentifier, AxiomItem<?>> items;



    public ItemValueImpl(AxiomTypeDefinition type, V value, Map<AxiomIdentifier, AxiomItem<?>> items) {
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
    public <T> Optional<AxiomItem<T>> item(AxiomIdentifier name) {
        return Optional.ofNullable((AxiomItem<T>) items.get(name));
    }

    @Override
    public Collection<AxiomItem<?>> items() {
        return items.values();
    }
}
