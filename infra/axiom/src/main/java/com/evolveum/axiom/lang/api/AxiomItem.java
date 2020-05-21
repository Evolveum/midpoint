package com.evolveum.axiom.lang.api;

import java.util.Collection;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

public interface AxiomItem<V> {

    AxiomIdentifier name();
    Optional<AxiomItemDefinition> definition();

    Collection<AxiomItemValue<V>> values();

    default AxiomItemValue<V> onlyValue() {
        return Iterables.getOnlyElement(values());
    }


}
