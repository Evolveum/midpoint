/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class IdentifierSpaceHolderImpl implements IdentifierSpaceHolder {

    Set<Scope> allowedScopes;
    Map<AxiomName, Map<AxiomValueIdentifier, ValueContext<?>>> space = new HashMap<>();

    public IdentifierSpaceHolderImpl(Scope first, Scope... rest) {
        allowedScopes = EnumSet.of(first, rest);
    }

    @Override
    public void register(AxiomName space, Scope scope, AxiomValueIdentifier key, ValueContext<?> item) {
        Preconditions.checkArgument(allowedScopes.contains(scope), "Scope " + scope + " is not allowed");// TODO
                                                                                                         // Auto-generated
                                                                                                         // method stub
        ValueContext<?> previous = space(space).putIfAbsent(key, item);
        if(previous != null) {
            throw AxiomSemanticException.create(item.startLocation(), "%s: %s is already defined at %s", space.localName(),item, previous.startLocation());
        }
    }

    @Override
    public ValueContext<?> lookup(AxiomName space, AxiomValueIdentifier key) {
        return space(space).get(key);
    }

    @Override
    public Map<AxiomValueIdentifier, ValueContext<?>> space(AxiomName spaceId) {
        return space.computeIfAbsent(spaceId, k -> new HashMap<>());
    }

    Map<AxiomName, Map<AxiomValueIdentifier, AxiomValue<?>>> build() {
        ImmutableMap.Builder<AxiomName, Map<AxiomValueIdentifier, AxiomValue<?>>> roots = ImmutableMap
                .builder();
        for (Entry<AxiomName, Map<AxiomValueIdentifier, ValueContext<?>>> entry : space.entrySet()) {
            ImmutableMap.Builder<AxiomValueIdentifier, AxiomValue<?>> space = ImmutableMap.builder();
            for (Entry<AxiomValueIdentifier, ValueContext<?>> item : entry.getValue().entrySet()) {
                space.put(item.getKey(), item.getValue().get());
            }
            roots.put(entry.getKey(), space.build());
        }
        return roots.build();
    }

}
