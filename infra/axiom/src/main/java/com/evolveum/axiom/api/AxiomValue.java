package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;


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
    default <T> Optional<AxiomItem<T>> item(AxiomName name) {
        return items().stream().filter(value -> name.equals(value.name())).findFirst().map(v -> (AxiomItem<T>) v);
    }

    @Override
    V get();

    static <V> AxiomValue<V> from(AxiomTypeDefinition typeDefinition, V value) {
        return new SimpleValue<V>(typeDefinition, value);
    }

   default <T> Optional<AxiomValue<T>> onlyValue(Class<T> type, AxiomItemDefinition... components) {
        Optional<AxiomValue<?>> current = Optional.of(this);
        for(AxiomItemDefinition name : components) {
            current = current.get().item(name).map(v -> v.onlyValue());
            if(current.isEmpty()) {
                return Optional.empty();
            }
        }
        return (Optional) current;
    }

}
