package com.evolveum.axiom.api;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.Lazy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class AxiomValueBuilder<V> implements Lazy.Supplier<AxiomValue<V>> {

    private AxiomTypeDefinition type;

    private AxiomValueFactory<V> factory;
    private Map<AxiomName, Supplier<? extends AxiomItem<?>>> children = new LinkedHashMap<>();
    private Map<AxiomName, Supplier<? extends AxiomItem<?>>> infra = new LinkedHashMap<>();
    private V value;

    public AxiomValueBuilder(AxiomTypeDefinition type, AxiomValueFactory<V> factory) {
        this.type = type;
        this.factory = factory;
    }

    public static <V> AxiomValueBuilder<V> from(AxiomTypeDefinition type) {
        return new AxiomValueBuilder<>(type, AxiomValueFactory.defaultFactory());
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

    public Supplier<? extends AxiomItem<?>> getInfra(AxiomName name, Function<AxiomName, ? extends Supplier<? extends AxiomItem<?>>> child) {
        return infra.computeIfAbsent(name, child);
    }

    @Override
    public AxiomValue<V> get() {
        if(type.isComplex()) {
            Builder<AxiomName, AxiomItem<?>> builder = ImmutableMap.builder();
            return (AxiomValue) factory.createComplex(type, build(children), build(infra));
        }
        Preconditions.checkState(children.isEmpty(), "%s does not have items. Items found %s", type.name(), children.keySet());
        return factory.createSimple(type, value, Collections.emptyMap());
    }

    private static Map<AxiomName,AxiomItem<?>> build(Map<AxiomName, Supplier<? extends AxiomItem<?>>> children) {
        Builder<AxiomName, AxiomItem<?>> builder = ImmutableMap.builder();
        for(Entry<AxiomName, Supplier<? extends AxiomItem<?>>> entry : children.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().get());
        }
        return builder.build();
    }

    public static <V> AxiomValueBuilder<V> create(AxiomTypeDefinition type, AxiomValueFactory<V> factory) {
        return new AxiomValueBuilder<>(type, factory);
    }

    public void setFactory(AxiomValueFactory<V> factoryFor) {
        this.factory = factoryFor;
    }

    public AxiomTypeDefinition type() {
        return type;
    }

    public void setType(AxiomTypeDefinition type) {
        this.type = type;
    }

}
