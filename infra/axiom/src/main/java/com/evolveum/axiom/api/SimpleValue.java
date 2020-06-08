package com.evolveum.axiom.api;

import java.util.Map;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public class SimpleValue<T> extends AbstractAxiomValue<T> implements AxiomSimpleValue<T> {

    private static final AxiomValueFactory FACTORY = SimpleValue::create;
    private final T value;

    SimpleValue(AxiomTypeDefinition type, T value, Map<AxiomName, AxiomItem<?>> infraItems) {
        super(type, infraItems);
        this.value = value;
    }

    public static final <V> AxiomSimpleValue<V> create(AxiomTypeDefinition def, V value, Map<AxiomName, AxiomItem<?>> infraItems) {
        return new SimpleValue<V>(def, value, infraItems);
    }

    @Override
    public T value() {
        return value;
    }

    public static <T> AxiomValueFactory<T, AxiomValue<T>> factory() {
        return FACTORY;
    }



}
