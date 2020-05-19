package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;

public interface StatementRule<V> {

    String name();

    boolean isApplicableTo(AxiomItemDefinition definition);

    void apply(StatementRuleContext<V> rule) throws AxiomSemanticException;

}
