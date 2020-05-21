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

public interface AxiomTypeDefinition extends AxiomNamedDefinition {

    public final AxiomIdentifier IDENTIFIER_MEMBER = AxiomIdentifier.axiom("name");
    public final AxiomIdentifier IDENTIFIER_SPACE = AxiomIdentifier.axiom("AxiomTypeDefinition");

    /*
    @Override
    default AxiomTypeDefinition get() {
        return this;
    }

    @Override
    default Optional<AxiomTypeDefinition> definition() {
        return Optional.empty();
    }*/

    Optional<AxiomItemDefinition> argument();

    Optional<AxiomTypeDefinition> superType();

    Map<AxiomIdentifier, AxiomItemDefinition> itemDefinitions();

    Collection<AxiomIdentifierDefinition> identifierDefinitions();

    default Optional<AxiomItemDefinition> itemDefinition(AxiomIdentifier child) {
        AxiomItemDefinition maybe = itemDefinitions().get(child);
        if(maybe != null) {
            return Optional.of(maybe);
        }
        if(superType().isPresent()) {
            return superType().get().itemDefinition(child);
        }
        return Optional.empty();
    }

    static IdentifierSpaceKey identifier(AxiomIdentifier name) {
        return IdentifierSpaceKey.from(ImmutableMap.of(IDENTIFIER_MEMBER, name));
    }

    default Collection<AxiomItemDefinition> requiredItems() {
        return itemDefinitions().values().stream().filter(AxiomItemDefinition::required).collect(Collectors.toList());
    }

}
