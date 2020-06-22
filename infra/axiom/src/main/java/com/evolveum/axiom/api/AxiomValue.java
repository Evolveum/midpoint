/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;


public interface AxiomValue<V> extends AxiomInfraValue {

    AxiomName AXIOM_VALUE = AxiomName.from(AxiomName.DATA_NAMESPACE, "AxiomValue");
    AxiomName TYPE = AXIOM_VALUE.localName("type");
    AxiomName VALUE = AXIOM_VALUE.localName("value");
    AxiomName METADATA = AXIOM_VALUE.localName("metadata");

    AxiomName METADATA_TYPE = AXIOM_VALUE.localName("ValueMetadata");


    Optional<AxiomTypeDefinition> type();

    V value();


    default Optional<? extends AxiomStructuredValue> metadata() {
        return infraItem(METADATA).flatMap(v -> v.onlyValue().asComplex());
    }

    default Optional<? extends AxiomItem<?>> metadata(AxiomName name) {
        return metadata().flatMap(m -> m.item(name));
    }

    default Optional<AxiomStructuredValue> asComplex() {
        if(this instanceof AxiomStructuredValue)  {
            return Optional.of((AxiomStructuredValue) this);
        }
        return Optional.empty();
    }

    interface Factory<V,T extends AxiomValue<V>> extends AxiomInfraValue.Factory<T> {

        @Override
        default T create(Map<AxiomName, AxiomItem<?>> infraItems) {
            AxiomTypeDefinition type = (AxiomTypeDefinition) infraItems.get(TYPE).onlyValue().value();
            V value = (V) infraItems.get(VALUE).onlyValue().value();
            return create(type, value, infraItems);
        }

        T create(AxiomTypeDefinition type, V value, Map<AxiomName, AxiomItem<?>> infraItems);

    }

}
