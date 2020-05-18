package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.impl.Requirement.Search;

public interface StatementRuleContext<V> {

    <V> Optional<V> optionalChildValue(AxiomItemDefinition supertypeReference, Class<V> type);

    <V> V requiredChildValue(AxiomItemDefinition supertypeReference, Class<V> type) throws AxiomSemanticException;

    V requireValue() throws AxiomSemanticException;

    StatementRuleContext<V> apply(StatementRuleContext.Action<V> action);

    StatementRuleContext<V> errorMessage(Supplier<RuleErrorMessage> errorFactory);

    RuleErrorMessage error(String format, Object... arguments);

    public interface Action<V> {

        void apply(StatementContext<V> context) throws AxiomSemanticException;
    }

    AxiomTypeDefinition typeDefinition();

    Optional<V> optionalValue();

    Search<AxiomStatement<?>> requireGlobalItem(AxiomIdentifier space, IdentifierSpaceKey key);

    Requirement<AxiomStatement<?>> requireChild(AxiomItemDefinition required);

    Requirement<NamespaceContext> requireNamespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId);

}
