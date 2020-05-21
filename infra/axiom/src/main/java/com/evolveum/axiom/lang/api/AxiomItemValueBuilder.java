package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.concepts.Lazy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.annotation.concurrent.NotThreadSafe;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

@NotThreadSafe
public class AxiomItemValueBuilder<V,T extends AxiomItemValue<V>> implements Lazy.Supplier<T> {

    private final AxiomTypeDefinition type;

    private final AxiomItemValueFactory<V,T> factory;
    private Map<AxiomIdentifier, Supplier<? extends AxiomItem<?>>> children = new LinkedHashMap<>();
    private V value;

    public AxiomItemValueBuilder(AxiomTypeDefinition type, AxiomItemValueFactory<V,T> factory) {
        this.type = type;
        this.factory = factory;
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

    @Override
    public T get() {
        Builder<AxiomIdentifier, AxiomItem<?>> builder = ImmutableMap.builder();
        for(Entry<AxiomIdentifier, Supplier<? extends AxiomItem<?>>> entry : children.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().get());
        }
        return factory.create(type, value, builder.build().values());
    }

}
