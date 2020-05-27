package com.evolveum.axiom.api;

import java.util.Map;

public interface AxiomValueFactory<V,T extends AxiomValue<V>> {

    T create(AxiomTypeDefinition def, V value, Map<AxiomIdentifier, AxiomItem<?>> items);
}

