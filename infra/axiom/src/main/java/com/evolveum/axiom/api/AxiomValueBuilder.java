package com.evolveum.axiom.api;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.impl.ItemValueImpl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class AxiomValueBuilder<V,T extends AxiomValue<V>> implements Lazy.Supplier<T> {

    private final AxiomTypeDefinition type;

    private AxiomValueFactory<V,T> factory;
    private Map<AxiomName, Supplier<? extends AxiomItem<?>>> children = new LinkedHashMap<>();
    private V value;

    public AxiomValueBuilder(AxiomTypeDefinition type, AxiomValueFactory<V,T> factory) {
        this.type = type;
        this.factory = factory;
    }

    public static <V> AxiomValueBuilder<V, AxiomValue<V>> from(AxiomTypeDefinition type) {
        return new AxiomValueBuilder(type, ItemValueImpl.factory());
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void add(AxiomName name, Supplier<? extends AxiomItem<?>> child) {
        children.put(name, child);
    }

    public Supplier<? extends AxiomItem<?>> get(AxiomName name) {
        return children.get(name);
    }

    public Supplier<? extends AxiomItem<?>> get(AxiomName name, Function<AxiomName, ? extends Supplier<? extends AxiomItem<?>>> child) {
        return children.computeIfAbsent(name, child);
    }

    @Override
    public T get() {
        Builder<AxiomName, AxiomItem<?>> builder = ImmutableMap.builder();
        for(Entry<AxiomName, Supplier<? extends AxiomItem<?>>> entry : children.entrySet()) {
            AxiomItem<?> item = entry.getValue().get();
            builder.put(entry.getKey(), entry.getValue().get());
        }
        return factory.create(type, value, builder.build());
    }

    public static <T,V extends AxiomValue<T>> AxiomValueBuilder<T,V> create(AxiomTypeDefinition type, AxiomValueFactory<T, V> factory) {
        return new AxiomValueBuilder<>(type, factory);
    }

    public void setFactory(AxiomValueFactory<V,T> factoryFor) {
        this.factory = factoryFor;
    }

    public AxiomTypeDefinition type() {
        return type;
    }

}
