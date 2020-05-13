package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.Identifiable;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;

import static com.evolveum.axiom.lang.api.AxiomBuiltIn.Item.*;


public class AxiomTypeDefinitionImpl extends AbstractAxiomBaseDefinition implements AxiomTypeDefinition {

    public static final Factory<AxiomIdentifier, AxiomTypeDefinitionImpl> FACTORY =AxiomTypeDefinitionImpl::new;
    private final Map<AxiomIdentifier, AxiomItemDefinition> items;
    private final Optional<AxiomTypeDefinition> superType;
    private Optional<AxiomItemDefinition> argument;

    public AxiomTypeDefinitionImpl(AxiomIdentifier keyword, AxiomIdentifier value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super(keyword, value, children, keywordMap);
        ImmutableMap.Builder<AxiomIdentifier, AxiomItemDefinition> builder =  ImmutableMap.builder();
        putAll(builder, children(ITEM_DEFINITION.name(), AxiomItemDefinition.class));
        items = builder.build();
        superType = first(SUPERTYPE_REFERENCE.name(), AxiomTypeDefinition.class);
        argument = firstValue(ARGUMENT.name(), AxiomIdentifier.class)
                .flatMap((AxiomIdentifier k) -> item(k));
    }

    @Override
    public Optional<AxiomItemDefinition> argument() {
        if (!argument.isPresent() && superType().isPresent()) {
            return superType().get().argument();
        }
        return argument;
    }

    @Override
    public Optional<AxiomTypeDefinition> superType() {
        return superType;
    }

    @Override
    public Map<AxiomIdentifier, AxiomItemDefinition> items() {
        return items;
    }

}
