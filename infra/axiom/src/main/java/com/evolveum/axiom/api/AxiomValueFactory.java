/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Map;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public interface AxiomValueFactory<V> {

    AxiomValueFactory<Object> DEFAULT_FACTORY = new AxiomValueFactory<Object>() {

        @Override
        public AxiomStructuredValue createComplex(AxiomTypeDefinition def, Map<AxiomName, AxiomItem<?>> items,
                Map<AxiomName, AxiomItem<?>> infraItems) {
            return new StructuredValueImpl(def, items, infraItems);
        }

        @Override
        public AxiomValue<Object> createSimple(AxiomTypeDefinition def, Object value,
                Map<AxiomName, AxiomItem<?>> infraItems) {
            return new SimpleValue<Object>(def, value, infraItems);
        }
    };

    AxiomValue<V> createSimple(AxiomTypeDefinition def, V value, Map<AxiomName, AxiomItem<?>> infraItems);

    AxiomStructuredValue createComplex(AxiomTypeDefinition def, Map<AxiomName, AxiomItem<?>> items ,Map<AxiomName, AxiomItem<?>> infraItems);

    @SuppressWarnings("unchecked")
    static <V> AxiomValueFactory<V> defaultFactory() {
        return (AxiomValueFactory<V>) DEFAULT_FACTORY;
    }

}
