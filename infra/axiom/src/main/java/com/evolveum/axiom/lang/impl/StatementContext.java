package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.reactor.Depedency;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;

public interface StatementContext<V> {

    V requireValue() throws AxiomSemanticException;

    AxiomItemDefinition definition();

    <V> StatementContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value);

    Optional<V> optionalValue();

    void replace(Depedency<AxiomStatement<?>> statement);

    StatementContext<?> parent();

    void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key);

    V requireValue(Class<V> type);


    void importIdentifierSpace(NamespaceContext namespaceContext);

    void exportIdentifierSpace(IdentifierSpaceKey namespace);

}
