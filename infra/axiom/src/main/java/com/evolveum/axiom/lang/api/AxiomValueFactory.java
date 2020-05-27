package com.evolveum.axiom.lang.api;

import java.util.Map;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomValueFactory<V,T extends AxiomValue<V>> {

    T create(AxiomTypeDefinition def, V value, Map<AxiomIdentifier, AxiomItem<?>> items);
}

