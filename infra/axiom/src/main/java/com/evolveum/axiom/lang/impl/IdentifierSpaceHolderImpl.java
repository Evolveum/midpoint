package com.evolveum.axiom.lang.impl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class IdentifierSpaceHolderImpl implements IdentifierSpaceHolder {

    Set<Scope> allowedScopes;
    Map<AxiomIdentifier, Map<IdentifierSpaceKey, ValueContext<?>>> space = new HashMap<>();

    public IdentifierSpaceHolderImpl(Scope first, Scope... rest) {
        allowedScopes = EnumSet.of(first, rest);
    }

    @Override
    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, ValueContext<?> item) {
        Preconditions.checkArgument(allowedScopes.contains(scope), "Scope " + scope + " is not allowed");// TODO
                                                                                                         // Auto-generated
                                                                                                         // method stub
        ValueContext<?> previous = space(space).putIfAbsent(key, item);
        if (previous != null) {
            throw new AxiomSemanticException(item.startLocation()
                    + Strings.lenientFormat("%s identifier space: Item %s is already defined at %s", space,
                            item, previous.startLocation()));
        }
    }

    @Override
    public ValueContext<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key) {
        return space(space).get(key);
    }

    @Override
    public Map<IdentifierSpaceKey, ValueContext<?>> space(AxiomIdentifier spaceId) {
        return space.computeIfAbsent(spaceId, k -> new HashMap<>());
    }

    Map<AxiomIdentifier, Map<IdentifierSpaceKey, AxiomValue<?>>> build() {
        ImmutableMap.Builder<AxiomIdentifier, Map<IdentifierSpaceKey, AxiomValue<?>>> roots = ImmutableMap
                .builder();
        for (Entry<AxiomIdentifier, Map<IdentifierSpaceKey, ValueContext<?>>> entry : space.entrySet()) {
            ImmutableMap.Builder<IdentifierSpaceKey, AxiomValue<?>> space = ImmutableMap.builder();
            for (Entry<IdentifierSpaceKey, ValueContext<?>> item : entry.getValue().entrySet()) {
                space.put(item.getKey(), item.getValue().get());
            }
            roots.put(entry.getKey(), space.build());
        }
        return roots.build();
    }

}
