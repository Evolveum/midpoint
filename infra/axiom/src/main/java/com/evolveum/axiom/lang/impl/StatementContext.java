package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;

public interface StatementContext<V> {

    V requireValue() throws AxiomSemanticException;

    AxiomItemDefinition definition();

    void registerAsGlobalItem(AxiomIdentifier typeName) throws AxiomSemanticException;

    <V> StatementContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value);

    Optional<V> optionalValue();

    void replace(Requirement<AxiomStatement<?>> statement);

    StatementContext<?> parent();

}
