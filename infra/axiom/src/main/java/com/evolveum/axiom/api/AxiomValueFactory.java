package com.evolveum.axiom.api;

import java.util.Map;

import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;

public interface AxiomValueFactory<V,T extends AxiomValue<V>> {

    T create(AxiomTypeDefinition def, V value, Map<AxiomIdentifier, AxiomItem<?>> items);
}

