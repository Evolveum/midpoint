package com.evolveum.axiom.api;

import java.util.Collection;

public interface AxiomItemFactory<V> {

    AxiomItem<V> create(AxiomItemDefinition def, Collection<? extends AxiomValue<?>> axiomItem);

}
