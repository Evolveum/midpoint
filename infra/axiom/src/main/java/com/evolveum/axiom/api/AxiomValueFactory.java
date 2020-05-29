package com.evolveum.axiom.api;

import java.util.Map;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public interface AxiomValueFactory<V,T extends AxiomValue<V>> {

    T create(AxiomTypeDefinition def, V value, Map<AxiomName, AxiomItem<?>> items);
}

