package com.evolveum.axiom.lang.impl;

import java.util.List;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.Multimap;

public class AxiomItemDefinitionImpl extends AbstractAxiomBaseDefinition implements AxiomItemDefinition {

    public static final Factory<AxiomIdentifier,AxiomItemDefinitionImpl> FACTORY = AxiomItemDefinitionImpl::new ;
    private final AxiomTypeDefinition type;
    private int minOccurs;

    public AxiomItemDefinitionImpl(AxiomIdentifier keyword, AxiomIdentifier value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super(keyword, value, children, keywordMap);
        type = first(AxiomBuiltIn.Item.TYPE_DEFINITION.name(), AxiomTypeDefinition.class)
                .orElseThrow(() -> new IllegalStateException("No 'type' declaration in " + super.toString()));
        minOccurs = firstValue(AxiomBuiltIn.Item.MIN_OCCURS.name(), String.class).map(Integer::parseInt).orElse(0);
    }

    @Override
    public AxiomTypeDefinition type() {
        return type;
    }

    @Override
    public boolean required() {
        return minOccurs > 0;
    }

    @Override
    public String toString() {
        return AxiomItemDefinition.toString(this);
    }

}
