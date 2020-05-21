package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.Dependency.Search;

public interface AxiomStatementRule<V> {

    String name();

    boolean isApplicableTo(AxiomItemDefinition definition);

    void apply(Context<V> rule) throws AxiomSemanticException;


    interface Context<V> {
        <V> Optional<V> optionalChildValue(AxiomItemDefinition supertypeReference, Class<V> type);

        <V> V requiredChildValue(AxiomItemDefinition supertypeReference, Class<V> type) throws AxiomSemanticException;

        V requireValue() throws AxiomSemanticException;

        Context<V> apply(Action<V> action);

        Context<V> errorMessage(Supplier<RuleErrorMessage> errorFactory);

        RuleErrorMessage error(String format, Object... arguments);

        AxiomTypeDefinition typeDefinition();

        AxiomItemDefinition itemDefinition();

        Optional<V> optionalValue();

        Search<AxiomStatement<?>> requireGlobalItem(AxiomIdentifier space, IdentifierSpaceKey key);

        Dependency<AxiomStatement<?>> requireChild(AxiomItemDefinition required);

        Dependency<NamespaceContext> requireNamespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId);

        Dependency<AxiomStatementContext<?>> modify(AxiomIdentifier identifierSpace, IdentifierSpaceKey identifier);

        Context<V> newAction();

        Dependency<AxiomStatement<?>> require(AxiomStatementContext<?> ext);
    }

    public interface Action<V> {

        void apply(AxiomStatementContext<V> context) throws AxiomSemanticException;
    }
}
