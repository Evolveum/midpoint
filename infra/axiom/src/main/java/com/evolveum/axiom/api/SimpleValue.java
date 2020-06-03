package com.evolveum.axiom.api;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public class SimpleValue<T> implements AxiomSimpleValue<T> {

    private static final AxiomValueFactory FACTORY = SimpleValue::create;
    private final AxiomTypeDefinition type;
    private final T value;

    SimpleValue(AxiomTypeDefinition type, T value) {
        super();
        this.type = type;
        this.value = value;
    }

    public static final <V> AxiomSimpleValue<V> create(AxiomTypeDefinition def, V value, Map<AxiomName, AxiomItem<?>> items) {
        return new SimpleValue<V>(def, value);
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public T value() {
        return value;
    }

    public static <T> AxiomValueFactory<T, AxiomValue<T>> factory() {
        return FACTORY;
    }



}
