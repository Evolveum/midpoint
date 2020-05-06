/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomTypeDefinition extends AxiomBaseDefinition {

    Optional<AxiomItemDefinition> argument();

    Optional<AxiomTypeDefinition> superType();

    Map<AxiomIdentifier, AxiomItemDefinition> items();

    default Optional<AxiomItemDefinition> item(AxiomIdentifier child) {
        AxiomItemDefinition maybe = items().get(child);
        if(maybe != null) {
            return Optional.of(maybe);
        }
        if(superType().isPresent()) {
            return superType().get().item(child);
        }
        return Optional.empty();
    }

}
