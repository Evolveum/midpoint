package com.evolveum.axiom.api;

import java.util.Optional;

class SimpleValue<T> implements AxiomValue<T> {

    private final AxiomTypeDefinition type;
    private final T value;



    SimpleValue(AxiomTypeDefinition type, T value) {
        super();
        this.type = type;
        this.value = value;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public T get() {
        return value;
    }

}
