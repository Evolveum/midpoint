package com.evolveum.axiom.lang.api;

import java.util.Collection;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomStatement<V> {

    AxiomIdentifier keyword();
    V value();

    Collection<AxiomStatement<?>> children();
    Collection<AxiomStatement<?>> children(AxiomIdentifier type);


}
