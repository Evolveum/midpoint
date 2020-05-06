/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api.stmt;

import java.util.Collection;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;

public interface AxiomStatement<V> {

    AxiomIdentifier keyword();
    V value();

    Collection<AxiomStatement<?>> children();



    Collection<AxiomStatement<?>> children(AxiomIdentifier name);

    default Optional<AxiomStatement<?>> first(AxiomItemDefinition item) {
        return first(item.identifier());
    }

    default Optional<AxiomStatement<?>> first(AxiomIdentifier name) {
        return children(name).stream().findFirst();
    }

}
