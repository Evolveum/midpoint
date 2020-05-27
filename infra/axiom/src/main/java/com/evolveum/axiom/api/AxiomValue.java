package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;


public interface AxiomValue<V> extends Supplier<V> {

    Optional<AxiomTypeDefinition> type();

    default Collection<AxiomItem<?>> items() {
        return Collections.emptyList();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    default Optional<AxiomItem<?>> item(AxiomItemDefinition def) {
        return (Optional) item(def.name());
    }

    @SuppressWarnings("unchecked")
    default <T> Optional<AxiomItem<T>> item(AxiomIdentifier name) {
        return items().stream().filter(value -> name.equals(value.name())).findFirst().map(v -> (AxiomItem<T>) v);
    }

    @Override
    V get();

    static <V> AxiomValue<V> from(AxiomTypeDefinition typeDefinition, V value) {
        return new SimpleValue<V>(typeDefinition, value);
    }

}
