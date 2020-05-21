package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.collect.Collections2;


public interface AxiomItemValue<V> extends Supplier<V> {

    Optional<AxiomTypeDefinition> definition();

    default Collection<AxiomItem<?>> items() {
        return Collections.emptyList();
    }

    default Collection<AxiomItem<?>> items(AxiomIdentifier name) {
        return Collections2.filter(items(), value -> name.equals(value.name()));
    }

    @Override
    V get();

    interface Factory<V,T extends AxiomItemValue<V>> {
        T create(AxiomTypeDefinition def, V value, Collection<AxiomItem<?>> items);
    }
}
