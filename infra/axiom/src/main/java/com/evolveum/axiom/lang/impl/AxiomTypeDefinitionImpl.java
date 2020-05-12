package com.evolveum.axiom.lang.impl;

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
import com.google.common.collect.Multimap;

import static com.evolveum.axiom.lang.api.AxiomBuiltIn.Item.*;


public class AxiomTypeDefinitionImpl extends AxiomStatementImpl<AxiomIdentifier> implements AxiomTypeDefinition {

    public static final Factory<AxiomIdentifier, AxiomTypeDefinitionImpl> FACTORY =AxiomTypeDefinitionImpl::new;
    private final Map<AxiomIdentifier, AxiomItemDefinition> items;

    public AxiomTypeDefinitionImpl(AxiomIdentifier keyword, AxiomIdentifier value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super(keyword, value, children, keywordMap);
        ImmutableMap.Builder<AxiomIdentifier, AxiomItemDefinition> builder =  ImmutableMap.builder();
        Identifiable.putAll(builder, children(PROPERTY_DEFINITION.identifier(), AxiomItemDefinition.class));
        Identifiable.putAll(builder, children(CONTAINER_DEFINITION.identifier(), AxiomItemDefinition.class));
        Identifiable.putAll(builder, children(REFERENCE_DEFINITION.identifier(), AxiomItemDefinition.class));
        items = builder.build();
    }

    @Override
    public AxiomIdentifier identifier() {
        return value();
    }

    @Override
    public String documentation() {
        return null;
    }

    @Override
    public Optional<AxiomItemDefinition> argument() {
        return first(ARGUMENT.identifier(), AxiomItemDefinition.class);
    }

    @Override
    public Optional<AxiomTypeDefinition> superType() {
        return first(SUPERTYPE_REFERENCE.identifier(), AxiomTypeDefinition.class);
    }

    @Override
    public Map<AxiomIdentifier, AxiomItemDefinition> items() {
        return items;
    }


}
