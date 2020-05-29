/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.google.common.collect.ImmutableMap;

public interface AxiomTypeDefinition extends AxiomNamedDefinition, AxiomValue<AxiomTypeDefinition> {

    public final AxiomName IDENTIFIER_MEMBER = AxiomName.axiom("name");
    public final AxiomName SPACE = AxiomName.axiom("AxiomTypeDefinition");


    @Override
    default AxiomTypeDefinition get() {
        return this;
    }

    @Override
    default Optional<AxiomTypeDefinition> type() {
        return Optional.empty();
    }

    Optional<AxiomItemDefinition> argument();

    Optional<AxiomTypeDefinition> superType();

    Map<AxiomName, AxiomItemDefinition> itemDefinitions();

    Collection<AxiomIdentifierDefinition> identifierDefinitions();

    default Optional<AxiomItemDefinition> itemDefinition(AxiomName child) {
        AxiomItemDefinition maybe = itemDefinitions().get(child);
        if(maybe == null) {
            maybe = itemDefinitions().get(Inheritance.adapt(name(), child));
        }
        if(maybe == null && child.namespace().isEmpty()) {
            maybe = itemDefinitions().get(name().localName(child.localName()));
        }
        return Optional.ofNullable(maybe).or(() -> superType().flatMap(s -> s.itemDefinition(child)));
    }

    static IdentifierSpaceKey identifier(AxiomName name) {
        return IdentifierSpaceKey.from(ImmutableMap.of(IDENTIFIER_MEMBER, name));
    }

    default Collection<AxiomItemDefinition> requiredItems() {
        return itemDefinitions().values().stream().filter(AxiomItemDefinition::required).collect(Collectors.toList());
    }

    default Optional<AxiomItemDefinition> itemDefinition(AxiomName parentItem, AxiomName name) {
        return itemDefinition(Inheritance.adapt(parentItem, name));
    }

    default boolean isSubtypeOf(AxiomTypeDefinition type) {
        return isSubtypeOf(type.name());
    }

    default boolean isSupertypeOf(AxiomTypeDefinition other) {
        return other.isSubtypeOf(this);
    }

    default boolean isSubtypeOf(AxiomName other) {
        Optional<AxiomTypeDefinition> current = Optional.of(this);
        while(current.isPresent()) {
            if(current.get().name().equals(other)) {
                return true;
            }
            current = current.get().superType();
        }
        return false;
    }

}
