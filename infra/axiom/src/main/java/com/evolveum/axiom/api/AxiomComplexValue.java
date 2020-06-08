package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public interface AxiomComplexValue extends AxiomValue<Collection<AxiomItem<?>>> {


    @Override
    default Collection<AxiomItem<?>> value() {
        return itemMap().values();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    default Optional<AxiomItem<?>> item(AxiomItemDefinition def) {
        return (Optional) item(def.name());
    }

    @SuppressWarnings("unchecked")
    default <T> Optional<AxiomItem<T>> item(AxiomName name) {
        return Optional.ofNullable((AxiomItem<T>) itemMap().get(name));
    }

   default <T> Optional<AxiomValue<T>> onlyValue(Class<T> type, AxiomItemDefinition... components) {
        Optional<AxiomValue<?>> current = Optional.of(this);
        for(AxiomItemDefinition name : components) {
            current = current.get().asComplex().flatMap(c -> c.item(name)).map(i -> i.onlyValue());
            if(!current.isPresent()) {
                return Optional.empty();
            }
        }
        return (Optional) current;
    }

    Map<AxiomName, AxiomItem<?>> itemMap();


    interface Factory extends AxiomValueFactory<Collection<AxiomItem<?>>> {

        @Override
        default AxiomValue<Collection<AxiomItem<?>>> createSimple(AxiomTypeDefinition def,
                Collection<AxiomItem<?>> value, Map<AxiomName, AxiomItem<?>> infraItems) {
            throw new IllegalStateException("Factory is only for complex types");
        }

    }

}
