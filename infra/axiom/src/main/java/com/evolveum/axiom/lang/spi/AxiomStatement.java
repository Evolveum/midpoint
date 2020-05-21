/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;

public interface AxiomStatement<V> {

    AxiomIdentifier keyword();
    V value();

    Collection<AxiomStatement<?>> children();

    Collection<AxiomStatement<?>> children(AxiomIdentifier name);

    default <T> Collection<T> children(AxiomIdentifier name, Class<T> type) {
        return children(name).stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    default <T> Optional<T> first(AxiomIdentifier axiomIdentifier, Class<T> class1) {
        return children(axiomIdentifier).stream().filter(class1::isInstance).findFirst().map(class1::cast);
    }

    default Optional<AxiomStatement<?>> first(AxiomItemDefinition item) {
        return first(item.name());
    }

    default Optional<AxiomStatement<?>> first(AxiomIdentifier name) {
        return children(name).stream().findFirst();
    }

    default <V> Optional<V> firstValue(AxiomIdentifier name, Class<V> type) {
        return children(name).stream().filter(s -> type.isInstance(s.value())).map(s -> type.cast(s.value())).findFirst();
    }

}
