package com.evolveum.axiom.lang.api;

import java.util.Collection;

public interface AxiomItemValueFactory<V,T extends AxiomItemValue<V>> {

    T create(AxiomTypeDefinition def, V value, Collection<AxiomItem<?>> items);
}

