/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.google.common.collect.Iterables;

public interface AxiomItem<V> {

    AxiomName name();

    Optional<AxiomItemDefinition> definition();

    Collection<? extends AxiomValue<V>> values();

    default AxiomValue<V> onlyValue() {
        return Iterables.getOnlyElement(values());
    }

    default Optional<? extends AxiomMapItem<V>> asMap() {
        if (this instanceof AxiomMapItem) {
            return Optional.of((AxiomMapItem<V>) this);
        }
        return Optional.empty();
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition def, Collection<? extends AxiomValue<V>> values) {
        if(def.identifierDefinition().isPresent()) {
            return AxiomMapItemImpl.from(def, values);
        }
        return AxiomItemImpl.from(def, values);
    }

    static <V> AxiomItem<V> from(AxiomItemDefinition def, AxiomValue<V> value) {
        return AxiomItemImpl.from(def, Collections.singleton(value));
    }

}
