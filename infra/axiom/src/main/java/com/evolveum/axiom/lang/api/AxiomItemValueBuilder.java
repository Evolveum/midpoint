package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.impl.ItemValueImpl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class AxiomItemValueBuilder<V,T extends AxiomItemValue<V>> implements Lazy.Supplier<T> {

    private final AxiomTypeDefinition type;

    private AxiomItemValueFactory<V,T> factory;
    private Map<AxiomIdentifier, Supplier<? extends AxiomItem<?>>> children = new LinkedHashMap<>();
    private V value;

    public AxiomItemValueBuilder(AxiomTypeDefinition type, AxiomItemValueFactory<V,T> factory) {
        this.type = type;
        this.factory = factory;
    }

    public static <V> AxiomItemValueBuilder<V, AxiomItemValue<V>> from(AxiomTypeDefinition type) {
        return new AxiomItemValueBuilder(type, ItemValueImpl.factory());
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void add(AxiomIdentifier name, Supplier<? extends AxiomItem<?>> child) {
        children.put(name, child);
    }

    public Supplier<? extends AxiomItem<?>> get(AxiomIdentifier name) {
        return children.get(name);
    }

    public Supplier<? extends AxiomItem<?>> get(AxiomIdentifier name, Function<AxiomIdentifier, ? extends Supplier<? extends AxiomItem<?>>> child) {
        return children.computeIfAbsent(name, child);
    }

    @Override
    public T get() {
        Builder<AxiomIdentifier, AxiomItem<?>> builder = ImmutableMap.builder();
        for(Entry<AxiomIdentifier, Supplier<? extends AxiomItem<?>>> entry : children.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().get());
        }
        return factory.create(type, value, builder.build());
    }

    public static <T,V extends AxiomItemValue<T>> AxiomItemValueBuilder<T,V> create(AxiomTypeDefinition type, AxiomItemValueFactory<T, V> factory) {
        return new AxiomItemValueBuilder<>(type, factory);
    }

    public void setFactory(AxiomItemValueFactory<V,T> factoryFor) {
        this.factory = factoryFor;
    }

    public AxiomTypeDefinition type() {
        return type;
    }

}
