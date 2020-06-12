/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.collect.ImmutableMap;

public interface AxiomTypeDefinition extends AxiomBaseDefinition {

    public final AxiomIdentifier IDENTIFIER_MEMBER = AxiomIdentifier.axiom("name");
    public final AxiomIdentifier IDENTIFIER_SPACE = AxiomIdentifier.axiom("AxiomTypeDefinition");

    Optional<AxiomItemDefinition> argument();

    Optional<AxiomTypeDefinition> superType();

    Map<AxiomIdentifier, AxiomItemDefinition> items();

    Collection<AxiomIdentifierDefinition> identifiers();

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

    static IdentifierSpaceKey identifier(AxiomIdentifier name) {
        return IdentifierSpaceKey.from(ImmutableMap.of(IDENTIFIER_MEMBER, name));
    }

    default Collection<AxiomItemDefinition> requiredItems() {
        return items().values().stream().filter(AxiomItemDefinition::required).collect(Collectors.toList());
    }

}
