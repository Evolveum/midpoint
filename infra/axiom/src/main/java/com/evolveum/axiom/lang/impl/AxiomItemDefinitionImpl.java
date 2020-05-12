package com.evolveum.axiom.lang.impl;

import java.util.List;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.Multimap;

public class AxiomItemDefinitionImpl extends AxiomStatementImpl<AxiomIdentifier> implements AxiomItemDefinition {

    public static final Factory<AxiomIdentifier,AxiomItemDefinitionImpl> FACTORY = AxiomItemDefinitionImpl::new ;

    public AxiomItemDefinitionImpl(AxiomIdentifier keyword, AxiomIdentifier value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super(keyword, value, children, keywordMap);
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
    public AxiomTypeDefinition type() {
        return first(AxiomBuiltIn.Item.TYPE_DEFINITION.identifier(), AxiomTypeDefinition.class).get();
    }

    @Override
    public boolean required() {
        return false;
    }


}
