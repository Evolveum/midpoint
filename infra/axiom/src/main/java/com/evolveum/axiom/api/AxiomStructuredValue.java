/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public interface AxiomStructuredValue extends AxiomValue<Collection<AxiomItem<?>>> {


    @Override
    default Collection<AxiomItem<?>> value() {
        return itemMap().values();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    default Optional<AxiomItem<?>> item(AxiomItemDefinition def) {
        return (Optional) item(def.name());
    }

    default Optional<? extends AxiomItem<?>> item(AxiomName name) {
        return Optional.ofNullable(itemMap().get(name));
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
