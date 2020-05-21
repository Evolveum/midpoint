package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Optional;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;

public interface AxiomStatementContext<V> {

    V requireValue() throws AxiomSemanticException;

    AxiomItemDefinition definition();

    <V> AxiomStatementContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value);

    Optional<V> optionalValue();

    void replace(Dependency<AxiomStatement<?>> statement);

    AxiomStatementContext<?> parent();

    void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key);

    V requireValue(Class<V> type);


    void importIdentifierSpace(NamespaceContext namespaceContext);

    void exportIdentifierSpace(IdentifierSpaceKey namespace);

    void addChildren(Collection<AxiomStatement<?>> children);

    AxiomStatementRule.Context<?> newAction(String actionName);

    AxiomStatementRule.Context<?> modify(AxiomStatementContext<?> target, String actionName);

    void addEffectiveChildren(Collection<AxiomStatement<?>> children);

}
