package com.evolveum.axiom.lang.api;

import java.util.Collection;

import com.evolveum.axiom.api.AxiomValue;

public interface AxiomItemFactory<V> {

    AxiomItem<V> create(AxiomItemDefinition def, Collection<? extends AxiomValue<?>> axiomItem);

}
