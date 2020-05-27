package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

class LazyValue<V> implements AxiomValue<V>{

    private final AxiomTypeDefinition type;
    private Object delegate;

    public LazyValue(AxiomTypeDefinition type, Supplier<AxiomValue<V>> supplier) {
        this.type = type;
        this.delegate = supplier;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.of(type);
    }

    @Override
    public V get() {
        if(delegate instanceof AxiomValue) {
            return ((AxiomValue<V>) delegate).get();
        }
        delegate = ((Supplier<AxiomValue<V>>)delegate).get();
        return get();
    }

}
