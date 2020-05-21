package com.evolveum.axiom.lang.spi;

import java.util.List;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomNamedDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.google.common.collect.Multimap;

public class AbstractAxiomBaseDefinition extends AxiomStatementImpl<AxiomIdentifier> implements AxiomNamedDefinition {

    public static final Factory<AxiomIdentifier,AbstractAxiomBaseDefinition> FACTORY = AbstractAxiomBaseDefinition::new ;
    private AxiomIdentifier name;
    private String documentation;

    public AbstractAxiomBaseDefinition(AxiomIdentifier keyword, AxiomIdentifier value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super(keyword, value, children, keywordMap);
        name = firstValue(AxiomBuiltIn.Item.NAME.name(), AxiomIdentifier.class).get();
    }

    @Override
    public AxiomIdentifier name() {
        return name;
    }

    @Override
    public String documentation() {
        return documentation;
    }

}
