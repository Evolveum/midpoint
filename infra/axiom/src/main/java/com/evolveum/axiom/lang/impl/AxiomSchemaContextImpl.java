package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.google.common.collect.ImmutableMap;

public class AxiomSchemaContextImpl implements AxiomSchemaContext {

    private Map<AxiomIdentifier, AxiomItemDefinition> roots;
    private Map<AxiomIdentifier, AxiomTypeDefinition> types;

    public AxiomSchemaContextImpl(Map<AxiomIdentifier, AxiomItemDefinition> roots,
            Map<AxiomIdentifier, AxiomTypeDefinition> types) {
        this.roots = roots;
        this.types = types;
    }

    public static AxiomSchemaContext of(Map<AxiomIdentifier, AxiomItemDefinition> roots,
            Map<AxiomIdentifier, AxiomTypeDefinition> types) {
        return new AxiomSchemaContextImpl(roots, types);
    }

    @Override
    public Collection<AxiomItemDefinition> roots() {
        return roots.values();
    }

    @Override
    public Optional<AxiomTypeDefinition> getType(AxiomIdentifier type) {
        return Optional.ofNullable(types.get(type));
    }

    @Override
    public Collection<AxiomTypeDefinition> types() {
        return types.values();
    }

    @Override
    public Optional<AxiomItemDefinition> getRoot(AxiomIdentifier type) {
        return Optional.ofNullable(roots.get(type));
    }

    public static AxiomSchemaContextImpl boostrapContext() {
        return new AxiomSchemaContextImpl(ImmutableMap.of(AxiomBuiltIn.Item.MODEL_DEFINITION.name(), AxiomBuiltIn.Item.MODEL_DEFINITION),
                ImmutableMap.of());
    }

    public static AxiomSchemaContext baseLanguageContext() {
        return ModelReactorContext.BASE_LANGUAGE.get();
    }
}
