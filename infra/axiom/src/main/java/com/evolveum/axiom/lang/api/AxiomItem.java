package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.collect.Iterables;

public interface AxiomItem<V> {

    AxiomIdentifier name();
    Optional<AxiomItemDefinition> definition();

    Collection<AxiomValue<V>> values();

    default AxiomValue<V> onlyValue() {
        return Iterables.getOnlyElement(values());
    }

    static <V> AxiomItem<V> of(AxiomItemDefinition def, V value) {
        return CompactAxiomItem.of(def, value);
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition def, Collection<? extends AxiomValue<V>> values) {
        return AxiomItemImpl.from(def, values);
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition def, AxiomValue<V> value) {
        return AxiomItemImpl.from(def, Collections.singleton(value));
    }


}
