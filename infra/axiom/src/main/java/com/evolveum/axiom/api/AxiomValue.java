package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;


public interface AxiomValue<V> {

    Optional<AxiomTypeDefinition> type();

    default Collection<AxiomItem<?>> metadata() {
        return Collections.emptyList();
    }


    V value();

    default Optional<AxiomComplexValue<V>> asComplex() {
        if(this instanceof AxiomComplexValue)  {
            return Optional.of((AxiomComplexValue<V>) this);
        }
        return Optional.empty();
    }

}
