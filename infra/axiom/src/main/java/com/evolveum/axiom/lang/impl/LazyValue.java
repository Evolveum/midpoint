package com.evolveum.axiom.lang.impl;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.LazyDelegate;

class LazyValue<V> extends LazyDelegate<AxiomValue<V>> implements AxiomValue<V> {

    private final AxiomTypeDefinition type;

    public LazyValue(AxiomTypeDefinition type, Supplier<AxiomValue<V>> supplier) {
        super(supplier::get);
        this.type = type;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.of(type);
    }

    @Override
    public V value() {
        return delegate().value();
    }

    @Override
    public Optional<AxiomComplexValue> asComplex() {
        return delegate().asComplex();
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> infraItems() {
        return delegate().infraItems();
    }

}
