/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class AxiomModelContext {

    private final Map<AxiomIdentifier, AxiomTypeDefinition> typeDefs;

    private AxiomModelContext(Map<AxiomIdentifier, AxiomTypeDefinition> typeDefs) {
        this.typeDefs = typeDefs;
    }


    public Optional<AxiomTypeDefinition> getType(AxiomIdentifier type) {
        return Optional.ofNullable(typeDefs.get(type));
    }

}
