package com.evolveum.axiom.lang.impl;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.spi.SourceLocation;

public interface StatementTreeBuilder {

    void setValue(Object value);

    Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement);

    AxiomIdentifier identifier();

    void setValue(Object value, SourceLocation loc);

    StatementTreeBuilder createChildNode(AxiomIdentifier identifier, SourceLocation loc);


}
