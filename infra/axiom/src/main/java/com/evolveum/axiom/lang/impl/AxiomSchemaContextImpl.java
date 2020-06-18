/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class AxiomSchemaContextImpl implements AxiomSchemaContext {

    private Map<AxiomValueIdentifier, AxiomItemDefinition> roots;
    private Map<AxiomValueIdentifier, AxiomTypeDefinition> types;
    private Map<AxiomName, Map<AxiomValueIdentifier, AxiomValue<?>>> globals;

    public AxiomSchemaContextImpl(Map<AxiomName,Map<AxiomValueIdentifier, AxiomValue<?>>> globalMap) {
        this.globals = globalMap;
        this.roots = Maps.transformValues(globalMap.get(AxiomItemDefinition.ROOT_SPACE), AxiomItemDefinition.class::cast);
        this.types = Maps.transformValues(globalMap.get(AxiomTypeDefinition.SPACE), AxiomTypeDefinition.class::cast);
    }

    @Override
    public Collection<AxiomItemDefinition> roots() {
        return roots.values();
    }

    @Override
    public Optional<AxiomTypeDefinition> getType(AxiomName type) {
        return Optional.ofNullable(types.get(nameKey(type)));
    }

    @Override
    public Collection<AxiomTypeDefinition> types() {
        return types.values();
    }

    @Override
    public Optional<AxiomItemDefinition> getRoot(AxiomName type) {
        return Optional.ofNullable(roots.get(nameKey(type)));
    }

    private static AxiomValueIdentifier nameKey(AxiomName type) {
        return AxiomValueIdentifier.of(AxiomTypeDefinition.IDENTIFIER_MEMBER, type);
    }

    public static AxiomSchemaContextImpl boostrapContext() {
        Map<AxiomValueIdentifier, AxiomValue<?>> root = ImmutableMap.of(nameKey(AxiomBuiltIn.Item.MODEL_DEFINITION.name()), AxiomBuiltIn.Item.MODEL_DEFINITION);
        Map<AxiomName, Map<AxiomValueIdentifier, AxiomValue<?>>> global
            = ImmutableMap.of(AxiomItemDefinition.ROOT_SPACE, root, AxiomTypeDefinition.SPACE, ImmutableMap.of());
        return new AxiomSchemaContextImpl(global);
    }

    public static AxiomSchemaContext baseLanguageContext() {
        return ModelReactorContext.BASE_LANGUAGE.get();
    }
}
