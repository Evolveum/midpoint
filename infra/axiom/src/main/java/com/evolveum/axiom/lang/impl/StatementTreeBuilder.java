package com.evolveum.axiom.lang.impl;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;

public interface StatementTreeBuilder {

    void setValue(Object value);

    Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement);

    AxiomIdentifier identifier();

    StatementTreeBuilder createChildNode(AxiomIdentifier identifier);

}
