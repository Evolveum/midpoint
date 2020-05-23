package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class AxiomSchemaContextImpl implements AxiomSchemaContext {

    private Map<IdentifierSpaceKey, AxiomItemDefinition> roots;
    private Map<IdentifierSpaceKey, AxiomTypeDefinition> types;
    private Map<AxiomIdentifier, Map<IdentifierSpaceKey, AxiomItemValue<?>>> globals;

    public AxiomSchemaContextImpl(Map<AxiomIdentifier,Map<IdentifierSpaceKey, AxiomItemValue<?>>> globalMap) {
        this.globals = globalMap;
        this.roots = Maps.transformValues(globalMap.get(AxiomItemDefinition.ROOT_SPACE), AxiomItemDefinition.class::cast);
        this.types = Maps.transformValues(globalMap.get(AxiomTypeDefinition.IDENTIFIER_SPACE), AxiomTypeDefinition.class::cast);
    }

    @Override
    public Collection<AxiomItemDefinition> roots() {
        return roots.values();
    }

    @Override
    public Optional<AxiomTypeDefinition> getType(AxiomIdentifier type) {
        return Optional.ofNullable(types.get(nameKey(type)));
    }

    @Override
    public Collection<AxiomTypeDefinition> types() {
        return types.values();
    }

    @Override
    public Optional<AxiomItemDefinition> getRoot(AxiomIdentifier type) {
        return Optional.ofNullable(roots.get(nameKey(type)));
    }

    private static IdentifierSpaceKey nameKey(AxiomIdentifier type) {
        return IdentifierSpaceKey.of(AxiomTypeDefinition.IDENTIFIER_MEMBER, type);
    }

    public static AxiomSchemaContextImpl boostrapContext() {
        Map<IdentifierSpaceKey, AxiomItemValue<?>> root = ImmutableMap.of(nameKey(AxiomBuiltIn.Item.MODEL_DEFINITION.name()), AxiomBuiltIn.Item.MODEL_DEFINITION);
        Map<AxiomIdentifier, Map<IdentifierSpaceKey, AxiomItemValue<?>>> global
            = ImmutableMap.of(AxiomItemDefinition.ROOT_SPACE, root, AxiomTypeDefinition.IDENTIFIER_SPACE, ImmutableMap.of());
        return new AxiomSchemaContextImpl(global);
    }

    public static AxiomSchemaContext baseLanguageContext() {
        return ModelReactorContext.BASE_LANGUAGE.get();
    }
}
